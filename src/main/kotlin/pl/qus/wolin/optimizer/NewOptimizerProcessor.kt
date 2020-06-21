package pl.qus.wolin.pl.qus.wolin.optimizer

import org.antlr.v4.runtime.ParserRuleContext
import pl.qus.wolin.MegaAsmParser
import pl.qus.wolin.PseudoAsmParser
import pl.qus.wolin.WolinStateObject
import pl.qus.wolin.components.Zmienna
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.Exception

class ReplacementPair(
    val thiz: FlowNode,
    val withThat: FlowNode
)
{
    override fun toString(): String {
        return "${thiz.uid}->${withThat.uid}"
    }
}

class NewOptimizerProcessor(private val finalState: WolinStateObject) {
    val allRegs = mutableListOf<FlowNode>()
    val redundantRegs = mutableListOf<FlowNode>()
    val pairs = mutableListOf<ReplacementPair>()

    val nonAssignOpcodes = listOf("bne", "beq", "label", "setup", "string")

    fun buildFlowTree(
        ctx: PseudoAsmParser.PseudoAsmFileContext
    ) {
        println("== gathering registers ==")
        ctx.linia()?.iterator()?.let { linie ->
            while (linie.hasNext()) {
                val line = linie.next()

                val opcode = line.instrukcja().simpleIdentifier().text

                if (opcode == "label" || opcode == "setup" || opcode == "function")
                    continue

                val targetContext = line.target(0)
                val leftContext = line.arg(0)
                val rightContext = line.arg(1)


                val leftRef = leftContext?.operand()?.referencer()?.firstOrNull()?.text ?: ""
                val rightRef = rightContext?.operand()?.referencer()?.firstOrNull()?.text ?: ""

                // TODO - źle contents jest brane z alloc/free i nie posiada ani typu, ani wektora
                val leftNode =
                    if (leftContext == null) null
                    else
                        obtain(leftContext).apply {
                            referenced = referenced || opcode != "free" && opcode != "alloc"
                            contents = if(leftContext.operand().children.size > contents?.children?.get(0)?.childCount ?: 0) leftContext else contents
                        }

                val rightNode =
                    if (rightContext == null) null
                    else {
                        obtain(rightContext).apply {
                            referenced = true
                            contents = if(rightContext.operand().children.size > contents?.children?.get(0)?.childCount ?: 0) leftContext else contents
                        }
                    }

                targetContext?.let { tc ->
                    val existing = allRegs.lastOrNull { it.uid == tc.getUid() }
                    existing?.referenced = true
                }

                if (targetContext != null && !nonAssignOpcodes.contains(opcode)/* && targetRef != "&"*/) {
                    obtainTarget(targetContext, opcode).apply {
                        referenced = true
                        incomingLeft = if (leftNode != null) DestPair(leftNode, leftRef) else null
                        incomingRight = if (rightNode != null) DestPair(rightNode, rightRef) else null
                        contents = targetContext
                    }
                } else if (opcode == "alloc" || opcode == "free") {
                    obtain(line.arg(0)).apply {
                        contents = if(contents==null) line.arg(0) else contents
                    }
                }
            }
        }


        // create tree
        allRegs.forEach {
            it.walkDownToSource(finalState)
            it.walkDownToSource(finalState)
        }

        dumpIntoGraphVizFile(allRegs, "initial")

        allRegs.filter { !it.referenced }.forEach {
            it.redundant = true
        }

        redundantRegs.addAll(allRegs.filter { it.redundant })

        allRegs.removeIf {
            it.redundant
        }
    }

    fun optimizeGraph() {

        // nowy:
        // jeżeli rejestr ma jedno podstawienie, a znajduje się też jako cel z &, to możemy zastąpić wszystkie & tymże podstawieniem
        // A = B
        // &A = C
        // &A = D
        // ....
        // Upraszcza się do:
        // &B = C
        // &B = D
        //
        // jeżeli
        // A = *B
        // to upraszcza się do:
        // B = C
        // B = D
        //
        // ale ponieważ przy A stoi &, to jest oznaczana jako don't optimize, więc jeśli jest pointerem i jeżeli jest tylko deref
        // to możemy jej skasować ten hint!


        // Let's enable optimization for pointers that are always dereferenced
        // 1. sprawdzamy, czy A jest pointerem
        // 2. czy ma tylko jedno podstawienie?
        // 3. czy po lewej stronie znajduje się tylko jako &?
        //

        allRegs.forEach {
            if(it.uid == "__wolin_reg4")
                println("tu!")
            val onlyDerefsRemain = remainingAssignsAreDerefs(it)
            val found = findInVariablary(it)
            val isPointer = found?.type?.isPointer

            if(onlyDerefsRemain && isPointer == true)
                found.isDereferenced = false
        }

        allRegs.firstOrNull { it.uid == "__wolin_reg4" }?.let {
            val prerequisites = optimizeAllowed(it) && it.isNode && it.hasOneInput
            val betterIsReg = it.incomingLeft?.node?.contents?.isReg() ?: false
            val cont = it.contents?.text

            println("tu!")
        }


        do {
            val redundant = allRegs.firstOrNull {
                val prerequisites = optimizeAllowed(it) && it.isNode && it.hasOneInput
                val betterIsReg = it.incomingLeft?.node?.contents?.isReg() ?: false

                // zamienienie
                // A = #B
                // C = A
                // musimy mieć pewność, że A nie jest po lewo jako &A
                val redundantAssignedOnce = isLvalueOnceIncludingDerefs(it)

                prerequisites && (
                        betterIsReg ||
                                redundantAssignedOnce
                        )
            }

            // jeżeli lepszy nie jest rejestrem, ale zastępowany jest niemutable, to można

            if (redundant != null) {
                redundant.incomingLeft?.let { better ->
                    allRegs.filter { it.uid == redundant.uid }.forEach { red ->
                        println("REPLACE ${red.uid} with ${better.node.uid} for ${red}")
                        try {
                            red.replaceWith(better, allRegs)
                            pairs.add(ReplacementPair(red, better.node))
                        } catch (ex: Exception) {
                            println("Failed! ${ex.message}")
                        }
                    }
                }

                redundantRegs.addAll(allRegs.filter { it.redundant })

                allRegs.removeIf {
                    it.redundant
                }
            }
        } while (redundant != null)

        // "backwards" replace
        do {
            val redundant = allRegs.firstOrNull {
                !it.hasOneInput && it.goesInto.size == 1 && isLvalueOnceNotCountingDerefs(it) && optimizeAllowed(it)
            }

            if (redundant != null) {
                val better = redundant.goesInto.first()
                println("BK REPLACE ${redundant.uid} with ${better.node.uid}")
                pairs.add(redundant.revReplaceWith(better, allRegs, isMutable(better.node)))

                redundantRegs.addAll(allRegs.filter { it.redundant })

                allRegs.removeIf {
                    it.redundant
                }
            }
        } while (redundant != null)

        dumpIntoGraphVizFile(allRegs, "optimized")
    }

    private fun findInVariablary(checked: FlowNode): Zmienna? {
        return finalState.variablary.values.firstOrNull { it.name == checked.uid }
    }

    private fun isMutable(checked: FlowNode): Boolean {
        val zmienna = findInVariablary(checked)
        val isReg = checked.contents!!.isReg()

        return when {
            !isReg -> false
            zmienna!=null -> zmienna.mutable
            else -> false
        }
    }

    private fun isLvalueOnceNotCountingDerefs(checked: FlowNode): Boolean =
        allRegs.filter { it.isTarget() }.count { it.uid == checked.uid && it.contents?.ref() != "&"} == 1

    private fun isLvalueOnceIncludingDerefs(checked: FlowNode): Boolean =
        allRegs.filter { it.isTarget() }.count { it.uid == checked.uid } == 1

    private fun remainingAssignsAreDerefs(checked: FlowNode): Boolean {
        val allAssigns = allRegs.filter { it.isTarget() }.count { it.uid == checked.uid }
        val allDerefs = allRegs.filter { it.isTarget() }.count { it.uid == checked.uid && it.contents?.ref() == "&" }

        return allAssigns - allDerefs == 1
    }

    private fun optimizeAllowed(b: FlowNode): Boolean {
        val zmienna = findInVariablary(b)
        return if (zmienna != null) !zmienna.isDereferenced else b.contents?.isReg() == true
    }


    fun removeRedundantAllocs(ctx: PseudoAsmParser.PseudoAsmFileContext, finalState: WolinStateObject) {
        println("== looking for allocs of redundant registers ==")

        // a może tak
        // 1. usuwamy alloc/free rejestrów better
        // 2. alloc/free rejestrów redundant zastępujemy przez better - pytanie, czy zawsze better jest rejestrem
        // 3. jeżeli coś zostało z redundant, to usuwamy

        ctx.linia()?.iterator()?.let { linie ->
            while (linie.hasNext()) {
                val line = linie.next()

                val opcode = line.instrukcja().simpleIdentifier().text
                val arg1Uid = (line.arg()?.getOrNull(0) as ParserRuleContext?)?.getUid()

                if ((opcode == "alloc" || opcode == "free") && redundantRegs.any { it.uid == arg1Uid }) {
                    println("usuwam: ${line.text}")
                    line.children.clear()
                }
            }
        }
    }

    fun replacePairs(ctx: PseudoAsmParser.PseudoAsmFileContext, finalState: WolinStateObject) {
        pairs.forEach { pair ->

            println("Pair: ${pair}")

            ctx.linia()?.iterator()?.let { linie ->
                while (linie.hasNext()) {
                    val line = linie.next()

                    val targetUid = (line.children.filter { it is PseudoAsmParser.TargetContext }.firstOrNull() as ParserRuleContext?)?.getUid()
                    val arg1Uid = (line.children.filter { it is PseudoAsmParser.ArgContext }.getOrNull(0) as ParserRuleContext?)?.getUid()
                    val arg2Uid = (line.children.filter { it is PseudoAsmParser.ArgContext }.getOrNull(1) as ParserRuleContext?)?.getUid()

                    if(targetUid == pair.thiz.uid) {
                        (line.children.filterIsInstance<PseudoAsmParser.TargetContext>().firstOrNull())?.children = pair.withThat.contents!!.children
                    }

                    if(arg1Uid == pair.thiz.uid) {
                        (line.children.filterIsInstance<PseudoAsmParser.ArgContext>().getOrNull(0))?.children = pair.withThat.contents!!.children
                    }

                    if(arg2Uid == pair.thiz.uid) {
                        (line.children.filterIsInstance<PseudoAsmParser.ArgContext>().getOrNull(1))?.children = pair.withThat.contents!!.children
                    }

                }
            }
        }
    }

    fun removeIdentities(ctx: PseudoAsmParser.PseudoAsmFileContext) {
        println("== looking for registers ==")
        ctx.linia()?.iterator()?.let { linie ->
            while (linie.hasNext()) {
                val line = linie.next()

                if (line.children.isNotEmpty()) {
                    val opcode = line.instrukcja().simpleIdentifier().text
                    val targetUid = (line.target()?.firstOrNull() as ParserRuleContext?)?.getUid()
                    val arg1Uid = (line.arg()?.getOrNull(0) as ParserRuleContext?)?.getUid()
                    val arg2Uid = (line.arg()?.getOrNull(1) as ParserRuleContext?)?.getUid()

                    if (opcode == "let" && arg2Uid == null) {
                        if (targetUid == arg1Uid) {
                            line.children.clear()
                        }
                    }
                }
            }
        }
    }

    fun obtain(c: ParserRuleContext): FlowNode {
        val existing = allRegs.lastOrNull { it.uid == c.getUid() }
        return if (existing != null)
            existing
        else {
            val nowy = FlowNode().apply {
                contents = c
            }
            allRegs.add(nowy)
            nowy
        }
    }

    fun obtainTarget(c: ParserRuleContext, opcode: String): FlowNode {
        val existing = allRegs.lastOrNull { it.uid == c.getUid() }
        return if (existing != null && (existing.incomingLeft == null || existing.incomingLeft?.ref == "&"))
            existing.apply { this.opcode = opcode } // pierwsze przypisanie
        else {
            val nowy =
                FlowNode(nodeNum = existing?.nodeNum?.inc() ?: 0).apply {
                    contents = c
                    this.opcode = opcode
                }
            allRegs.add(nowy)
            nowy
        }
    }

    private fun dumpIntoGraphVizFile(newRegs: List<FlowNode>, nazwa: String) {
        // bin\dot -Tpng graph.dot > output.png
        val ostream = OutputStreamWriter(FileOutputStream(File("src/main/wolin/$nazwa.dot")))
        ostream.write(
            "digraph mygraph{\n" +
                    " node [shape=plaintext]\n"
        )

        newRegs.forEach {
            if (it.incomingLeft != null) {
                val label = "${it.opcode}\nv=[${it.incomingLeft?.ref}],..."
                ostream.write("\"{${it.incomingLeft!!.node.contents!!.getUid()}}\" -> \"{${it.contents!!.getUid()}}\"")
                ostream.write("[label=\"${label}\"]")
            }
            if (it.incomingRight != null) {
                val label = "${it.opcode}\nv=...,[${it.incomingRight?.ref}]"
                ostream.write("\"{${it.incomingRight!!.node.contents!!.getUid()}}\" -> \"{${it.contents!!.getUid()}}\"")
                ostream.write("[label=\"${label}\"]")
            }
        }

        ostream.write("}\n")
        ostream.close()
    }

}

fun ParserRuleContext.ref(): String {
    return when (this) {
        null -> ""
        is PseudoAsmParser.TargetContext -> {
            this.operand()?.referencer()?.firstOrNull()?.text ?: ""
        }
        is PseudoAsmParser.ArgContext -> {
            this.operand()?.referencer()?.firstOrNull()?.text ?: ""
        }
        else -> throw java.lang.Exception("Nie wiem, jak to przerobić: ${this.javaClass.name}")
    }
}

fun ParserRuleContext.stripRefs() {
    when (this) {
        is PseudoAsmParser.TargetContext -> {
            (this.children.firstOrNull { it is PseudoAsmParser.OperandContext } as PseudoAsmParser.OperandContext?)?.children?.removeAll { it is PseudoAsmParser.ReferencerContext }
        }
        is PseudoAsmParser.ArgContext -> {
            (this.children.firstOrNull { it is PseudoAsmParser.OperandContext } as PseudoAsmParser.OperandContext?)?.children?.removeAll { it is PseudoAsmParser.ReferencerContext }
        }
    }
}

fun ParserRuleContext.getUid(): String {
    return when (this) {
        null -> ""
        is PseudoAsmParser.TargetContext -> {
            if (this.operand().name(0)?.identifier()?.isEmpty == false)
                this.operand().name(0).identifier().text
            else if (!this.operand().value().isEmpty)
                this.operand().value().text
            else
                throw java.lang.Exception("Nie wiem, jak przerobić ${this.text}")

        }
        is PseudoAsmParser.ArgContext -> {
            if (this.operand().name(0)?.identifier()?.isEmpty == false)
                this.operand().name(0).identifier().text
            else if (!this.operand().value().isEmpty)
                this.operand().value().text
            else
                throw java.lang.Exception("Nie wiem, jak przerobić ${this.text}")
        }
        else -> throw java.lang.Exception("Nie wiem, jak to przerobić")
    }
}

fun ParserRuleContext.isReg(): Boolean {
    return when (this) {
        is PseudoAsmParser.TargetContext -> {
            val stos = this.operand()?.value()?.addressed()?.identifier()?.simpleIdentifier(0)?.text ?: ""
            val nazwa = this.operand()?.name(0)?.identifier()?.simpleIdentifier(0)?.text ?: ""

            return stos.isNotEmpty() && nazwa.isNotEmpty()
        }
        is PseudoAsmParser.ArgContext -> {
            val stos = this.operand()?.value()?.addressed()?.identifier()?.simpleIdentifier(0)?.text ?: ""
            val nazwa = this.operand()?.name(0)?.identifier()?.simpleIdentifier(0)?.text ?: ""

            return stos.isNotEmpty() && nazwa.isNotEmpty()
        }
        else -> {
            false
        }
    }
}

