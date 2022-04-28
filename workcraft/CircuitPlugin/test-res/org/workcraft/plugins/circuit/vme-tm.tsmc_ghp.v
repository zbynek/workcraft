// Verilog netlist generated by Workcraft 3
module VME (dsr, dsw, ldtack, d, lds, dtack);
    input dsr, dsw, ldtack;
    output d, lds, dtack;
    wire U1_ON, IN_BUBBLE3_ON, IN_BUBBLE5_ON, U7_ON, IN_BUBBLE10_ON, OUT_BUBBLE1_ON, U14_ON, IN_BUBBLE16_ON, IN_BUBBLE18_ON, U20_ON, IN_BUBBLE23_ON, IN_BUBBLE25_ON, IN_BUBBLE28_ON, OUT_BUBBLE2_ON, U31_ON, IN_BUBBLE33_ON, OUT_BUBBLE3_ON, U36_ON;

    IND3D1 U1 (.ZN(U1_ON), .A1(OUT_BUBBLE3_ON), .B1(ldtack), .B2(dsr));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE3 (.ZN(IN_BUBBLE3_ON), .I(OUT_BUBBLE2_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE5 (.ZN(IN_BUBBLE5_ON), .I(ldtack));
    OAI221D1 U7 (.ZN(U7_ON), .A1(IN_BUBBLE3_ON), .A2(d), .B1(IN_BUBBLE5_ON), .B2(OUT_BUBBLE3_ON), .C(dsw));
    ND2D1 U8 (.ZN(d), .A1(U7_ON), .A2(U1_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE10 (.ZN(IN_BUBBLE10_ON), .I(OUT_BUBBLE3_ON));
    INVD1 OUT_BUBBLE1 (.ZN(OUT_BUBBLE1_ON), .I(U14_ON));
    OAI221D1 U14 (.ZN(U14_ON), .A1(d), .A2(dsr), .B1(dsr), .B2(OUT_BUBBLE2_ON), .C(IN_BUBBLE10_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE16 (.ZN(IN_BUBBLE16_ON), .I(OUT_BUBBLE2_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE18 (.ZN(IN_BUBBLE18_ON), .I(dsw));
    OAI31D1 U20 (.ZN(U20_ON), .A1(IN_BUBBLE18_ON), .A2(IN_BUBBLE16_ON), .A3(d), .B(OUT_BUBBLE3_ON));
    C2 U21 (.Q(lds), .A(U20_ON), .B(OUT_BUBBLE1_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE23 (.ZN(IN_BUBBLE23_ON), .I(OUT_BUBBLE3_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE25 (.ZN(IN_BUBBLE25_ON), .I(OUT_BUBBLE2_ON));
    AOI221D1 U26 (.ZN(dtack), .A1(IN_BUBBLE23_ON), .A2(dsw), .B1(d), .B2(OUT_BUBBLE3_ON), .C(IN_BUBBLE25_ON));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE28 (.ZN(IN_BUBBLE28_ON), .I(OUT_BUBBLE3_ON));
    INVD1 OUT_BUBBLE2 (.ZN(OUT_BUBBLE2_ON), .I(U31_ON));
    OAI222D1 U31 (.ZN(U31_ON), .A1(IN_BUBBLE28_ON), .A2(dsw), .B1(OUT_BUBBLE2_ON), .B2(d), .C1(d), .C2(lds));
    // This inverter should have a short delay
    INVD1 IN_BUBBLE33 (.ZN(IN_BUBBLE33_ON), .I(d));
    INVD1 OUT_BUBBLE3 (.ZN(OUT_BUBBLE3_ON), .I(U36_ON));
    AOI32D1 U36 (.ZN(U36_ON), .A1(IN_BUBBLE33_ON), .A2(ldtack), .A3(OUT_BUBBLE2_ON), .B1(ldtack), .B2(OUT_BUBBLE3_ON));

    // signal values at the initial state:
    // IN_BUBBLE10_ON IN_BUBBLE16_ON IN_BUBBLE18_ON IN_BUBBLE23_ON IN_BUBBLE25_ON IN_BUBBLE28_ON IN_BUBBLE33_ON IN_BUBBLE3_ON IN_BUBBLE5_ON !OUT_BUBBLE1_ON !OUT_BUBBLE2_ON !OUT_BUBBLE3_ON U14_ON U1_ON U20_ON U31_ON U36_ON U7_ON !d !dsr !dsw !dtack !lds !ldtack
endmodule
