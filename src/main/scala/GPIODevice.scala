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
  address: BigInt = 0x5000,
  width: Int = 32,
  useAXI4: Boolean = true)

case object GPIODeviceKey extends Field[Option[GPIODeviceParams]](None)

class GPIOTopIO() extends Bundle {
  val gpio = Output(Bool())
}

class GPIOAXI4(params: GPIODeviceParams, beatBytes: Int)(implicit p: Parameters) extends ClockSinkDomain(ClockSinkParameters())(p) {
  // val device = new SimpleDevice("gpio", Seq("ucbbar,gpio"))
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    slaves = Seq(AXI4SlaveParameters(
      address = Seq(AddressSet(params.address, 512-1)), 
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
    val io = IO(new GPIOTopIO())

    withClockAndReset(clock, reset) {
      val blackbox = Module(new axi_gpio_0)
      blackbox.io.s_axi_aclk := clock
      blackbox.io.s_axi_aresetn := ~(reset.asBool)
      val (axi_async, _) = node.in(0)

      blackbox.io.s_axi_awaddr := axi_async.aw.bits.addr - params.address.U
      blackbox.io.s_axi_awvalid := axi_async.aw.valid
      axi_async.aw.ready := blackbox.io.s_axi_awready

      blackbox.io.s_axi_wdata := axi_async.w.bits.data
      blackbox.io.s_axi_wstrb := axi_async.w.bits.strb
      blackbox.io.s_axi_wvalid := axi_async.w.valid
      axi_async.w.ready := blackbox.io.s_axi_wready

      axi_async.b.valid := blackbox.io.s_axi_bvalid
      axi_async.b.bits.resp := blackbox.io.s_axi_bresp
      blackbox.io.s_axi_bready := axi_async.b.ready

      axi_async.ar.ready := blackbox.io.s_axi_arready
      blackbox.io.s_axi_araddr := axi_async.ar.bits.addr - params.address.U
      blackbox.io.s_axi_arvalid := axi_async.ar.valid
      axi_async.ar.bits.id := 0.U

      axi_async.r.valid := blackbox.io.s_axi_rvalid
      axi_async.r.bits.data := blackbox.io.s_axi_rdata
      axi_async.r.bits.resp := blackbox.io.s_axi_rresp
      blackbox.io.s_axi_rready := axi_async.r.ready
      axi_async.r.bits.last := true.B

      io.gpio := blackbox.io.gpio_io_o(0)
      blackbox.io.gpio_io_i := 0.U
    }
  }
}

class axi_gpio_0 extends BlackBox() {
  val io = IO(new Bundle {
    val s_axi_aclk = Input(Clock())
    val s_axi_aresetn = Input(Reset())

    val s_axi_awaddr = Input(UInt(9.W))
    val s_axi_awvalid = Input(Bool())
    val s_axi_awready = Output(Bool())

    val s_axi_wdata = Input(UInt(32.W))
    val s_axi_wstrb = Input(UInt(4.W))
    val s_axi_wvalid = Input(Bool())
    val s_axi_wready = Output(Bool())

    val s_axi_bresp = Output(UInt(2.W))
    val s_axi_bvalid = Output(Bool())
    val s_axi_bready = Input(Bool())

    val s_axi_araddr = Input(UInt(9.W))
    val s_axi_arvalid = Input(Bool())
    val s_axi_arready = Output(Bool())

    val s_axi_rdata = Output(UInt(32.W))
    val s_axi_rresp = Output(UInt(2.W))
    val s_axi_rvalid = Output(Bool())
    val s_axi_rready = Input(Bool())

    val gpio_io_i = Input(UInt(32.W))
    val gpio_io_o = Output(UInt(32.W))
    val gpio_io_t = Output(UInt(32.W))
  })
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
        val gpio_io = IO(Bool()).suggestName("gpio_top_io_pin")
        gpio_io := gpio.module.io.gpio
        gpio_io
      }
      Some(gpio_top)
    }
    case None => None
  }

  
}

class WithPeripheryGPIO extends Config((site, here, up) => {
  case GPIODeviceKey => Some(GPIODeviceParams())
})
