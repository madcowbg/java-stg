.class public Main
.super java/lang/Object

;
; standard initializer (calls java.lang.Object's initializer)
;
.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method

;
; main() - prints out Hello World
;
.method public static main([Ljava/lang/String;)V
    .limit stack 2   ; up to two items can be pushed
    .limit locals 2

    ; push System.out onto the stack
    getstatic java/lang/System/out Ljava/io/PrintStream;

    ; push a string onto the stack
    ldc "Hello World!"

    ; call the PrintStream.println() method.

    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

    bipush 5
    bipush 37
    iadd
    istore 0

    iload 0
    invokestatic java/lang/String/valueOf(I)Ljava/lang/String;

    astore 1

    getstatic java/lang/System/out Ljava/io/PrintStream;
    aload 1

    invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

    ; done
    return
.end method