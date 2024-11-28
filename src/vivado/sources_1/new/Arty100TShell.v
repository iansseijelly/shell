`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 11/26/2024 10:54:24 AM
// Design Name: 
// Module Name: Arty100TShell
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////

module Arty100TShell(
  input CLK100MHZ,
  input ck_rst,
  output uart_rxd_out,
  input uart_txd_in,

  output jd_0,
  input jd_1,
  input jd_2,
  input jd_3,
  input jd_4,
  input jd_5,
  input jd_6,
  input jd_7,

  output led0_b,
  output led1_b,
  output led2_b
);


  wire clock;
  wire pll_locked;
  wire reset;
    
  clk_wiz_0 u_clk_wiz_0 (
    .clk_in1(CLK100MHZ),
    .reset(~ck_rst),
    .locked(pll_locked),
    .clk_out1(clock)
  );
  
  
  sync_reset #(
    .N(2)
  )
  u_sync_reset (
    .clk(clock),
    .rst(~pll_locked),
    .out(reset)
  );
 


  assign led1_b = 1'b1;
  assign led2_b = 1'b0;

  wire cbus_reset;
  wire jtag_reset;
  
  sync_reset #(
    .N(2)
  )
  u_sync_debug_reset (
    .clk(jd_2),
    .rst(cbus_reset),
    .out(jtag_reset)
  );

  DigitalTop system (
    .auto_chipyard_prcictrl_domain_reset_setter_clock_in_member_allClocks_uncore_clock (clock),
    .auto_chipyard_prcictrl_domain_reset_setter_clock_in_member_allClocks_uncore_reset (reset),
    .auto_mbus_fixedClockNode_anon_out_clock                                           (),
    .auto_cbus_fixedClockNode_anon_out_clock                                           (),
    .auto_cbus_fixedClockNode_anon_out_reset                                           (cbus_reset),
    .resetctrl_hartIsInReset_0                                                         (cbus_reset),
    .debug_clock                                                                       (clock),
    .debug_reset                                                                       (reset),
    .debug_systemjtag_jtag_TCK                                                         (jd_2),
    .debug_systemjtag_jtag_TMS                                                         (jd_5),
    .debug_systemjtag_jtag_TDI                                                         (jd_4),
    .debug_systemjtag_jtag_TDO_data                                                    (jd_0),
    .debug_systemjtag_reset                                                            (jtag_reset),
    .debug_dmactive                                                                    (),
    .debug_dmactiveAck                                                                 ('b1),
    .mem_axi4_0_aw_ready                                                               ('b1),
    .mem_axi4_0_aw_valid                                                               (),
    .mem_axi4_0_aw_bits_id                                                             (),
    .mem_axi4_0_aw_bits_addr                                                           (),
    .mem_axi4_0_aw_bits_len                                                            (),
    .mem_axi4_0_aw_bits_size                                                           (),
    .mem_axi4_0_aw_bits_burst                                                          (),
    .mem_axi4_0_aw_bits_lock                                                           (),
    .mem_axi4_0_aw_bits_cache                                                          (),
    .mem_axi4_0_aw_bits_prot                                                           (),
    .mem_axi4_0_aw_bits_qos                                                            (),
    .mem_axi4_0_w_ready                                                                ('b1),
    .mem_axi4_0_w_valid                                                                (),
    .mem_axi4_0_w_bits_data                                                            (),
    .mem_axi4_0_w_bits_strb                                                            (),
    .mem_axi4_0_w_bits_last                                                            (),
    .mem_axi4_0_b_ready                                                                (),
    .mem_axi4_0_b_valid                                                                ('b0),
    .mem_axi4_0_b_bits_id                                                              ('b0),
    .mem_axi4_0_b_bits_resp                                                            ('b0),
    .mem_axi4_0_ar_ready                                                               ('b1),
    .mem_axi4_0_ar_valid                                                               (),
    .mem_axi4_0_ar_bits_id                                                             (),
    .mem_axi4_0_ar_bits_addr                                                           (),
    .mem_axi4_0_ar_bits_len                                                            (),
    .mem_axi4_0_ar_bits_size                                                           (),
    .mem_axi4_0_ar_bits_burst                                                          (),
    .mem_axi4_0_ar_bits_lock                                                           (),
    .mem_axi4_0_ar_bits_cache                                                          (),
    .mem_axi4_0_ar_bits_prot                                                           (),
    .mem_axi4_0_ar_bits_qos                                                            (),
    .mem_axi4_0_r_ready                                                                (),
    .mem_axi4_0_r_valid                                                                ('b0),
    .mem_axi4_0_r_bits_id                                                              ('b0),
    .mem_axi4_0_r_bits_data                                                            ('b0),
    .mem_axi4_0_r_bits_resp                                                            ('b0),
    .mem_axi4_0_r_bits_last                                                            ('b0),
    .custom_boot                                                                       ('b1),
    .serial_tl_0_in_ready                                                              (),
    .serial_tl_0_in_valid                                                              ('b0),
    .serial_tl_0_in_bits_phit                                                          ('b0),
    .serial_tl_0_out_ready                                                             ('b0),
    .serial_tl_0_out_valid                                                             (),
    .serial_tl_0_out_bits_phit                                                         (),
    .serial_tl_0_clock_in                                                              ('b0),
    .uart_0_txd                                                                        (uart_rxd_out),
    .uart_0_rxd                                                                        (uart_txd_in),
    .gcd_busy                                                                          (),
    .clock_tap                                                                         (),
    .gpio_top_io_pin                                                                   (led0_b)
  );

endmodule
