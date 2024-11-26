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
  val node = AXI4RegisterNode(
    AddressSet(params.address, 4096-1), 
    beatBytes = beatBytes
  )
  override lazy val module = new GPIODeviceImpl

  class GPIODeviceImpl extends Impl {
    val io = IO(new GPIOTopIO())
    withClockAndReset(clock, reset) {
      val gpios = RegInit(false.B)
      val counter = Counter(1000000)

      when(counter.value === 0.U) {
        gpios := !gpios
      }
      counter.inc()

      io.gpio := gpios

      node.regmap(
        0x00 -> Seq(
          RegField.r(1, gpios)
        )
      )
    }
  }
}

trait CanHavePeripheryGPIO { this: BaseSubsystem =>
  private val portName = "gpio"
  private val pbus = locateTLBusWrapper(PBUS)

  val gpio_top = p(GPIODeviceKey) match {
    case Some(params) => {
      val gpio = LazyModule(new GPIOAXI4(params, pbus.beatBytes)(p))
      gpio.clockNode := pbus.fixedClockNode
      pbus.coupleTo(portName) {
      gpio.node :=
      AXI4Buffer() :=
      TLToAXI4() :=
      TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true) := _
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
