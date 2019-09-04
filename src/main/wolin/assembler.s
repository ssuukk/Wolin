; setupHEADER


;**********************************************
;*
;* BASIC header
;*
;* compile with:
;* cl65.exe -o assembler.prg -t c64 -C c64-asm.cfg -g -Ln labels.txt assembler.s
;*
;**********************************************
            .org 2049
            .export LOADADDR = *
Bas10:      .word BasEnd
            .word 10
            .byte 158 ; sys
            .byte " 2064"
            .byte 0
BasEnd:     .word 0
            .word 0
            ;


; setupSPF=251[ubyte],40959[uword]


; prepare function stack
__wolin_spf := 251 ; function stack ptr
__wolin_spf_hi := 251+1 ; function stack ptr

__wolin_spf_top := 40959 ; function stack top
__wolin_spf_top_hi := 40959+1 ; function stack top
    lda #<__wolin_spf_top ; set function stack top
    sta __wolin_spf
    lda #>__wolin_spf_top
    sta __wolin_spf+1

; setupSP=143[ubyte]


; prepare program stack
__wolin_sp_top := 143 ; program stack top
__wolin_sp_top_hi := 143+1 ; program stack top
    ldx #__wolin_sp_top ; set program stack top

; setupHEAP=176[ubyte]


__wolin_this_ptr := 176
__wolin_this_ptr_hi := 176+1


; allocSP<__wolin_reg0>,#1

    dex

; letSP(0)<__wolin_reg0>[ubyte]=#0[ubyte]


    lda #0
    sta 0,x

; let__wolin_pl_qus_wolin_znak<pl.qus.wolin.znak>[ubyte]=SP(0)<__wolin_reg0>[ubyte]


    lda 0,x
    sta __wolin_pl_qus_wolin_znak


; freeSP<__wolin_reg0>,#1

    inx

; allocSP<__wolin_reg1>,#1

    dex

; letSP(0)<__wolin_reg1>[ubyte]=#0[ubyte]


    lda #0
    sta 0,x

; let__wolin_pl_qus_wolin_i<pl.qus.wolin.i>[uword]=SP(0)<__wolin_reg1>[ubyte]


    lda 0,x
    sta __wolin_pl_qus_wolin_i
    lda #0
    sta __wolin_pl_qus_wolin_i+1


; freeSP<__wolin_reg1>,#1

    inx

; allocSPF,#0

 

; call__wolin_pl_qus_wolin_main[adr]

    jsr __wolin_pl_qus_wolin_main

; freeSPF<unit>,#0

 

; ret

    rts

; label__wolin_pl_qus_wolin_allocMem

__wolin_pl_qus_wolin_allocMem:

; allocSP<__wolin_reg2>,#2


    dex
    dex

; allocSP<__wolin_reg3>,#2


    dex
    dex

; letSP(0)<__wolin_reg3>[adr]=#30000[uword]


    lda #<30000
    sta 0,x
    lda #>30000
    sta 0+1,x

; letSPF(4)<returnValue>[adr]=SP(0)<__wolin_reg3>[adr]


    lda 0,x
    ldy #4
    sta (__wolin_spf),y
    lda 0+1,x
    iny
    sta (__wolin_spf),y

; freeSP<__wolin_reg3>,#2


    inx
    inx

; freeSP<__wolin_reg2>,#2


    inx
    inx

; freeSPF,#4


    clc
    lda __wolin_spf
    adc #4
    sta __wolin_spf
    bcc :+
    inc __wolin_spf+1
:

; ret

    rts

; label__wolin_pl_qus_wolin_Test

__wolin_pl_qus_wolin_Test:

; allocSP<__wolin_reg4>,#2


    dex
    dex

; allocSPF,#6


    clc
    lda __wolin_spf
    sbc #6
    sta __wolin_spf
    bcs :+
    dec __wolin_spf+1
:

; letSPF(2)[uword]=#3[uword]


    ldy #2
    lda #<3
    sta (__wolin_spf),y
    iny
    lda #>3
    sta (__wolin_spf),y

; letSPF(0)[uword]=#1[uword]


    ldy #0
    lda #<1
    sta (__wolin_spf),y
    iny
    lda #>1
    sta (__wolin_spf),y

; call__wolin_pl_qus_wolin_allocMem[adr]

    jsr __wolin_pl_qus_wolin_allocMem

; letSP(0)<__wolin_reg4>[adr]=SPF(0)<returnValue>[adr]


    ldy #0
    lda (__wolin_spf),y
    sta 0,x
    iny
    lda (__wolin_spf),y
    sta 0+1,x

; freeSPF<Any>,#2


    clc
    lda __wolin_spf
    adc #2
    sta __wolin_spf
    bcc :+
    inc __wolin_spf+1
:

; letSPF(0)<pl.qus.wolin.Test.returnValue>[adr]=SP(0)<__wolin_reg4>[adr]


    lda 0,x
    ldy #0
    sta (__wolin_spf),y
    lda 0+1,x
    iny
    sta (__wolin_spf),y

; setupHEAP=SP(0)<__wolin_reg4>[adr]


    lda 0,x
    sta __wolin_this_ptr
    lda 0+1,x
    sta __wolin_this_ptr+1

; freeSP<__wolin_reg4>,#2


    inx
    inx

; allocSP<__wolin_reg5>,#1

    dex

; letSP(0)<__wolin_reg5>[ubyte]=#3[ubyte]


    lda #3
    sta 0,x

; letHEAP(2)<pl.qus.wolin.Test.x>[ubyte]=SP(0)<__wolin_reg5>[ubyte]


    lda 0,x
    ldy #2
    sta (__wolin_this_ptr),y

; freeSP<__wolin_reg5>,#1

    inx

; allocSP<__wolin_reg6>,#1

    dex

; letSP(0)<__wolin_reg6>[ubyte]=#7[ubyte]


    lda #7
    sta 0,x

; letHEAP(1)<pl.qus.wolin.Test.y>[ubyte]=SP(0)<__wolin_reg6>[ubyte]


    lda 0,x
    ldy #1
    sta (__wolin_this_ptr),y

; freeSP<__wolin_reg6>,#1

    inx

; ret

    rts

; label__wolin_pl_qus_wolin_Test_suma

__wolin_pl_qus_wolin_Test_suma:

; setupHEAP=this


    ldy #0 ; this pointer from SPF to this pointer on ZP
    lda (__wolin_spf),y
    sta __wolin_this_ptr
    iny
    lda (__wolin_spf),y
    sta __wolin_this_ptr+1

; allocSP<__wolin_reg8>,#1

    dex

; letSP(0)<__wolin_reg8>[ubyte]=HEAP(2)<pl.qus.wolin.Test.x>[ubyte]


    ldy #2 ; assuming this ZP is set!
    lda (__wolin_this_ptr),y
    sta 0,x

; allocSP<__wolin_reg9>,#1

    dex

; letSP(0)<__wolin_reg9>[ubyte]=HEAP(1)<pl.qus.wolin.Test.y>[ubyte]


    ldy #1 ; assuming this ZP is set!
    lda (__wolin_this_ptr),y
    sta 0,x

; addSP(1)<__wolin_reg8>[ubyte]=SP(1)<__wolin_reg8>[ubyte],SP(0)<__wolin_reg9>[ubyte]


    clc
    lda 1,x
    adc 0,x
    sta 1,x

; freeSP<__wolin_reg9>,#1

    inx

; letSPF(2)<returnValue>[ubyte]=SP(0)<__wolin_reg8>[ubyte]


    lda 0,x
    ldy #2
    sta (__wolin_spf),y

; freeSP<__wolin_reg8>,#1

    inx

; freeSPF,#2


    clc
    lda __wolin_spf
    adc #2
    sta __wolin_spf
    bcc :+
    inc __wolin_spf+1
:

; ret

    rts

; label__wolin_pl_qus_wolin_main

__wolin_pl_qus_wolin_main:

; allocSP<__wolin_reg12>,#1

    dex

; label__wolin_lab_loopStart_1

__wolin_lab_loopStart_1:

; allocSP<__wolin_reg13>,#2


    dex
    dex

; letSP(0)<__wolin_reg13>[uword]=__wolin_pl_qus_wolin_i<pl.qus.wolin.i>[uword]


    lda __wolin_pl_qus_wolin_i
    sta 0,x
    lda __wolin_pl_qus_wolin_i+1
    sta 0+1,x

; allocSP<__wolin_reg14>,#2


    dex
    dex

; letSP(0)<__wolin_reg14>[uword]=#1000[uword]


    lda #<1000
    sta 0,x
    lda #>1000
    sta 0+1,x

; evallessSP(4)<__wolin_reg12>[bool]=SP(2)<__wolin_reg13>[uword],SP(0)<__wolin_reg14>[uword]


    lda #1 ; mniejsze
    sta 4,x
    lda 2+1,x
    cmp 0+1,x
    bcc :+
    lda 2,x
    cmp 0,x
    bcc :+
    lda #0 ; jednak wieksze
    sta 4,x
:

; freeSP<__wolin_reg14>,#2


    inx
    inx

; freeSP<__wolin_reg13>,#2


    inx
    inx

; bneSP(0)<__wolin_reg12>[bool]=#1[bool],__wolin_lab_loopEnd_1<label_po_if>[adr]


    lda 0,x
    beq __wolin_lab_loopEnd_1

; allocSP<__wolin_reg17>,#2


    dex
    dex

; allocSP<__wolin_reg18>,#2


    dex
    dex

; letSP(0)<__wolin_reg18>[adr]=1024[adr]


    lda #<1024
    sta 0,x
    lda #>1024
    sta 0+1,x

; allocSP<__wolin_reg19>,#2


    dex
    dex

; letSP(0)<__wolin_reg19>[uword]=__wolin_pl_qus_wolin_i<pl.qus.wolin.i>[uword]


    lda __wolin_pl_qus_wolin_i
    sta 0,x
    lda __wolin_pl_qus_wolin_i+1
    sta 0+1,x

; add__wolin_pl_qus_wolin_i<pl.qus.wolin.i>[uword]=__wolin_pl_qus_wolin_i<pl.qus.wolin.i>[uword],#1[uword]


    clc
    lda __wolin_pl_qus_wolin_i
    adc #<1
    sta __wolin_pl_qus_wolin_i
    lda __wolin_pl_qus_wolin_i+1
    adc #>1
    sta __wolin_pl_qus_wolin_i+1


; addSP(2)<__wolin_reg18>[adr]=SP(2)<__wolin_reg18>[adr],SP(0)<__wolin_reg19>[uword]


    clc
    lda 2,x
    adc 0,x
    sta 2,x
    lda 2+1,x
    adc 0+1,x
    sta 2+1,x

; freeSP<__wolin_reg19>,#2


    inx
    inx

; letSP(2)<__wolin_reg17>[adr]=SP(0)<__wolin_reg18>[adr]


    lda 0,x
    sta 2,x
    lda 0+1,x
    sta 2+1,x

; freeSP<__wolin_reg18>,#2


    inx
    inx

; allocSP<__wolin_reg20>,#1

    dex

; letSP(0)<__wolin_reg20>[ubyte]=__wolin_pl_qus_wolin_znak<pl.qus.wolin.znak>[ubyte]


    lda __wolin_pl_qus_wolin_znak
    sta 0,x

; add__wolin_pl_qus_wolin_znak<pl.qus.wolin.znak>[ubyte]=__wolin_pl_qus_wolin_znak<pl.qus.wolin.znak>[ubyte],#1[ubyte]


    inc __wolin_pl_qus_wolin_znak

; letSP(1)<__wolin_reg17>[ptr]=SP(0)<__wolin_reg20>[ubyte]


    lda 0,x
    sta (1,x)


; freeSP<__wolin_reg20>,#1

    inx

; freeSP<__wolin_reg17>,#2


    inx
    inx

; goto__wolin_lab_loopStart_1[adr]

    jmp __wolin_lab_loopStart_1

; label__wolin_lab_loopEnd_1

__wolin_lab_loopEnd_1:

; freeSP<__wolin_reg12>,#1

    inx

; allocSP<__wolin_reg22>,#2


    dex
    dex

; letSP(0)<__wolin_reg22>[adr]=53280[adr]


    lda #<53280
    sta 0,x
    lda #>53280
    sta 0+1,x

; allocSP<__wolin_reg23>,#1

    dex

; letSP(0)<__wolin_reg23>[ubyte]=#8[ubyte]


    lda #8
    sta 0,x

; letSP(1)<__wolin_reg22>[ptr]=SP(0)<__wolin_reg23>[ubyte]


    lda 0,x
    sta (1,x)


; freeSP<__wolin_reg23>,#1

    inx

; freeSP<__wolin_reg22>,#2


    inx
    inx

; ret

    rts

; label__wolin_indirect_jsr

__wolin_indirect_jsr:

; goto65535[adr]

    jmp 65535

; label__wolin_pl_qus_wolin_znak

__wolin_pl_qus_wolin_znak:

; alloc0[ubyte]

    .byte 0

; label__wolin_pl_qus_wolin_i

__wolin_pl_qus_wolin_i:

; alloc0[uword]

    .word 0

