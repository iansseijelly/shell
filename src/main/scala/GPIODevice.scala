package shell

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.prci._
import freechips.rocketchip.subsystem._

case class GPIODeviceParams(
  address: BigInt = 0x11000000,
  width: Int = 32,
  useAXI4: Boolean = true)

case object GPIODeviceKey extends Field[Option[GPIODeviceParams]](None)

class AXI_TOP_IO extends Bundle {
  val s_axi_aclk = Output(Clock())
  val s_axi_aresetn = Output(Bool())

  val s_axi_awaddr = Output(UInt(9.W))
  val s_axi_awvalid = Output(Bool())
  val s_axi_awready = Input(Bool())

  val s_axi_wdata = Output(UInt(32.W))
  val s_axi_wstrb = Output(UInt(4.W))
  val s_axi_wvalid = Output(Bool())
  val s_axi_wready = Input(Bool())

  val s_axi_bresp = Input(UInt(2.W))
  val s_axi_bvalid = Input(Bool())
  val s_axi_bready = Output(Bool())

  val s_axi_araddr = Output(UInt(9.W))
  val s_axi_arvalid = Output(Bool())
  val s_axi_arready = Input(Bool())

  val s_axi_rdata = Input(UInt(32.W))
  val s_axi_rresp = Input(UInt(2.W))
  val s_axi_rvalid = Input(Bool())
  val s_axi_rready = Output(Bool())
}

class GPIOAXI4(params: GPIODeviceParams, beatBytes: Int)(implicit p: Parameters) extends ClockSinkDomain(ClockSinkParameters())(p) {
  // val device = new SimpleDevice("gpio", Seq("ucbbar,gpio"))
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    slaves = Seq(AXI4SlaveParameters(
      address = Seq(AddressSet(params.address, 0x01000000-1)), 
      executable = false,
      supportsWrite = TransferSizes(1, beatBytes),
      supportsRead = TransferSizes(1, beatBytes),
      interleavedId = Some(0)
    )),
    beatBytes = beatBytes,
    minLatency = 1
  )))
  override lazy val module = new GPIODeviceImpl

  class GPIODeviceImpl extends Impl {
    val io = IO(new AXI_TOP_IO())

    withClockAndReset(clock, reset) {
      io.s_axi_aclk := clock
      io.s_axi_aresetn := ~(reset.asBool)
      val (axi_async, _) = node.in(0)
      axi_async.w.ready := io.s_axi_wready

      io.s_axi_awaddr := axi_async.aw.bits.addr - params.address.U
      io.s_axi_awvalid := axi_async.aw.valid
      axi_async.aw.ready := io.s_axi_awready

      io.s_axi_wdata := axi_async.w.bits.data
      io.s_axi_wstrb := axi_async.w.bits.strb
      io.s_axi_wvalid := axi_async.w.valid

      axi_async.b.valid := io.s_axi_bvalid
      axi_async.b.bits.resp := io.s_axi_bresp
      io.s_axi_bready := axi_async.b.ready

      axi_async.ar.ready := io.s_axi_arready
      io.s_axi_araddr := axi_async.ar.bits.addr - params.address.U
      io.s_axi_arvalid := axi_async.ar.valid
      axi_async.ar.bits.id := 0.U

      axi_async.r.valid := io.s_axi_rvalid
      axi_async.r.bits.data := io.s_axi_rdata
      axi_async.r.bits.resp := io.s_axi_rresp
      io.s_axi_rready := axi_async.r.ready
      axi_async.r.bits.last := true.B
    }
  }
}


trait CanHavePeripheryGPIO { this: BaseSubsystem =>
  private val portName = "gpio"
  private val pbus = locateTLBusWrapper(PBUS)

  val gpio_top = p(GPIODeviceKey) match {
    case Some(params) => {
      val gpio = LazyModule(new GPIOAXI4(params, 4)(p))
      gpio.clockNode := pbus.fixedClockNode
      pbus.coupleTo(portName) {
      gpio.node :=
      AXI4Buffer() :=
      AXI4UserYanker() :=
      // AXI4Deinterleaver() :=
      TLToAXI4() :=
      TLFragmenter(4, pbus.blockBytes, holdFirstDeny = true) :=
      TLWidthWidget(pbus.beatBytes) :=
      TLBuffer() := _
      }
      val gpio_top = InModuleBody {
        val gpio_axi_top_io = IO(new AXI_TOP_IO()).suggestName("gpio_axi_top_io")
        gpio_axi_top_io <> gpio.module.io
        gpio_axi_top_io
      }
      Some(gpio_top)
    }
    case None => None
  }

  
}

class WithPeripheryGPIO(address: BigInt = 0x20000000) extends Config((site, here, up) => {
  case GPIODeviceKey => Some(GPIODeviceParams(address = address))
})
