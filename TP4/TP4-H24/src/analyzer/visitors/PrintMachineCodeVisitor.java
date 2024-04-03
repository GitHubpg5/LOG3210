package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.*;

public class PrintMachineCodeVisitor implements ParserVisitor {
    private PrintWriter m_writer = null;

    private int MAX_REGISTERS_COUNT = 256;

    private final ArrayList<String> RETURNS = new ArrayList<>();
    private final ArrayList<MachineCodeLine> CODE = new ArrayList<>();

    private final ArrayList<String> MODIFIED = new ArrayList<>();
    private final ArrayList<String> REGISTERS = new ArrayList<>();

    private final HashMap<String, String> OPERATIONS = new HashMap<>();

    public PrintMachineCodeVisitor(PrintWriter writer) {
        m_writer = writer;

        OPERATIONS.put("+", "ADD");
        OPERATIONS.put("-", "MIN");
        OPERATIONS.put("*", "MUL");
        OPERATIONS.put("/", "DIV");
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, null);

        computeLifeVar();
        computeNextUse();

        printMachineCode();

        return null;
    }

    @Override
    public Object visit(ASTNumberRegister node, Object data) {
        MAX_REGISTERS_COUNT = ((ASTIntValue) node.jjtGetChild(0)).getValue();
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            RETURNS.add(((ASTIdentifier) node.jjtGetChild(i)).getValue());
        }
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, null);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // TODO (ex1): Modify CODE to add the needed MachLine.
        // Here the type of Assignment is "assigned = left op right".
        // You can pass null as data to children.
        String op = node.getOp();
        String assignation = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String gauche = (String) node.jjtGetChild(1).jjtAccept(this, null);
        String droite = (String) node.jjtGetChild(2).jjtAccept(this, null);

        MachineCodeLine machineCodeLine = new MachineCodeLine(op, assignation, gauche, droite);
        CODE.add(machineCodeLine);

        return null;
    }

    @Override
    public Object visit(ASTAssignUnaryStmt node, Object data) {
        // TODO (ex1): Modify CODE to add the needed MachLine.
        // Here the type of Assignment is "assigned = - right".
        // Suppose the left part to be the constant "#O".
        // You can pass null as data to children.

        String assignation = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String droite = (String) node.jjtGetChild(1).jjtAccept(this, null);

        MachineCodeLine machineCodeLine = new MachineCodeLine("-", assignation, "", droite);
        CODE.add(machineCodeLine);

        return null;
    }

    @Override
    public Object visit(ASTAssignDirectStmt node, Object data) {
        // TODO (ex1): Modify CODE to add the needed MachLine.
        // Here the type of Assignment is "assigned = right".
        // Suppose the left part to be the constant "#O".
        // You can pass null as data to children.

        String assignation = (String) node.jjtGetChild(0).jjtAccept(this, null);
        String droite = (String) node.jjtGetChild(1).jjtAccept(this, null);

        MachineCodeLine machineCodeLine = new MachineCodeLine("uh", assignation, "#0", droite);
        CODE.add(machineCodeLine);
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, null);
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return "#" + node.getValue();
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        return node.getValue();
    }

    private void computeLifeVar() {
        // TODO (ex2): Implement life variables algorithm on the CODE array.
        for (int i = 0; i < CODE.size(); i++)
        {
//            IN[CODE.get(i)] = {};
            CODE.get(i).Life_IN.clear();

//            OUT[CODE.get(i)] = {};
            CODE.get(i).Life_OUT.clear();
        }
//        OUT[CODE.size()-1] = Returned_Values;
        CODE.get(CODE.size()-1).Life_OUT = new HashSet<>(RETURNS);
        for (int i = CODE.size()-1; i >= 0; i--) {
            if (i < (CODE.size() - 1)) {
//                OUT[CODE.get(i)] = IN[CODE.get(i + 1)];
                CODE.get(i).Life_OUT = CODE.get(i+1).Life_IN;
            }
            HashSet<String> temp = new HashSet<>(CODE.get(i).Life_OUT);
            // (OUT[nodeList[i]] - DEF[nodeList[i]]
            temp.removeAll(CODE.get(i).DEF);
            // union REF[nodeList[i]]
            temp.addAll(CODE.get(i).REF);
            // IN[nodeList[i]] =
            CODE.get(i).Life_IN = temp;
            //System.out.println(CODE.get(i).Life_IN);
        }
    }

    private void computeNextUse() {
        // TODO (ex3): Implement next-use algorithm on the CODE array.
        for(int i = 0; i < CODE.size(); i++)
        {
            CODE.get(i).Next_IN.nextUse.clear();
            CODE.get(i).Next_OUT.nextUse.clear();
        }
        for(int i = CODE.size() - 1; i >= 0; i--)
        {
            if(i < (CODE.size() - 1)){
                CODE.get(i).Next_OUT = (NextUse) CODE.get(i + 1).Next_IN.clone();
            }
            int finalI = i;
            CODE.get(i).Next_OUT.nextUse.forEach((v, n) -> {
                if(!CODE.get(finalI).DEF.contains(v)) {
                    CODE.get(finalI).Next_IN.nextUse.put(v, (ArrayList<Integer>) n.clone()) ;
                }});
            CODE.get(i).REF.forEach((ref) -> CODE.get(finalI).Next_IN.add(ref, finalI));
            System.out.println(CODE.get(i).REF);
        }
    }

    /**
     * This function should generate the LD and ST when needed.
     */
    public String chooseRegister(String variable, HashSet<String> life, NextUse next, boolean loadIfNotFound) {
        // TODO (ex4): if variable is a constant (starts with '#'), return variable
        // TODO (ex4): if REGISTERS contains variable, return "R" + index
        // TODO (ex4): if REGISTERS size is not max (< MAX_REGISTERS_COUNT), add variable to REGISTERS and return "R" + index
        // TODO (ex4): if REGISTERS has max size:
        // - put variable in space of an other variable which is not used anymore
        // *or*
        // - put variable in space of variable which as the largest next-use
        if (variable.charAt(0) == '#')
            return variable;
        if(REGISTERS.contains(variable))
            return "R" + REGISTERS.indexOf(variable);
        if(REGISTERS.size() < MAX_REGISTERS_COUNT){
            REGISTERS.add(variable);
            if(loadIfNotFound) m_writer.println("LD " + "R" + (REGISTERS.size()-1 )+ ", " + variable);
            return "R" + REGISTERS.indexOf(variable);
        }
        if(REGISTERS.size() == MAX_REGISTERS_COUNT){
            String replacedVar = "";
            int maxVal = 0;
            for(String var: REGISTERS)
            {
                if(!next.nextUse.containsKey(var))
                {
                    replacedVar = var;
                    break;
                }
                for (int val : next.nextUse.get(var)) {
                    if (val > maxVal) {
                        maxVal = val;
                        replacedVar = var;
                    }
                }
            }
            if(loadIfNotFound) m_writer.println("LD " + "R" + REGISTERS.indexOf(replacedVar)+ ", " + variable);
            return "R" + REGISTERS.indexOf(replacedVar);
        }

        // Le dernier cas est si le Register depasse le maximum pour une raison quelconque.
        return null;
    }

    /**
     * Print the machine code in the output file
     */
    public void printMachineCode() {
        // TODO (ex4): Print the machine code in the output file.
        // You should change the code below.
        for (int i = 0; i < CODE.size(); i++) {
            m_writer.println("// Step " + i);
            String gauche = chooseRegister(CODE.get(i).LEFT, CODE.get(i).Life_IN, CODE.get(i).Next_IN, true);
            String droite = chooseRegister(CODE.get(i).RIGHT, CODE.get(i).Life_IN, CODE.get(i).Next_IN, true);
            String assignation = chooseRegister(CODE.get(i).ASSIGN, CODE.get(i).Life_OUT, CODE.get(i).Next_OUT, false);

            m_writer.println(CODE.get(i).OPERATION + " " + assignation + ", " + gauche + ", " + droite);
            m_writer.println(CODE.get(i));
        }
    }

    /**
     * Order a set in alphabetic order
     *
     * @param set The set to order
     * @return The ordered list
     */
    public List<String> orderedSet(Set<String> set) {
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    /**
     * A class to store and manage next uses.
     */
    private class NextUse {
        public HashMap<String, ArrayList<Integer>> nextUse = new HashMap<>();

        public NextUse() {}

        public NextUse(HashMap<String, ArrayList<Integer>> nextUse) {
            this.nextUse = nextUse;
        }

        public ArrayList<Integer> get(String s) {
            return nextUse.get(s);
        }

        public void add(String s, int i) {
            if (!nextUse.containsKey(s)) {
                nextUse.put(s, new ArrayList<>());
            }
            nextUse.get(s).add(i);
        }

        public String toString() {
            ArrayList<String> items = new ArrayList<>();
            for (String key : orderedSet(nextUse.keySet())) {
                Collections.sort(nextUse.get(key));
                items.add(String.format("%s:%s", key, nextUse.get(key)));
            }
            return String.join(", ", items);
        }

        @Override
        public Object clone() {
            return new NextUse((HashMap<String, ArrayList<Integer>>) nextUse.clone());
        }
    }

    /**
     * A struct to store the data of a machine code line.
     */
    private class MachineCodeLine {
        String OPERATION;
        String ASSIGN;
        String LEFT;
        String RIGHT;

        public HashSet<String> REF = new HashSet<>();
        public HashSet<String> DEF = new HashSet<>();

        public HashSet<String> Life_IN = new HashSet<>();
        public HashSet<String> Life_OUT = new HashSet<>();

        public NextUse Next_IN = new NextUse();
        public NextUse Next_OUT = new NextUse();

        public MachineCodeLine(String operation, String assign, String left, String right) {
            this.OPERATION = OPERATIONS.get(operation);
            this.ASSIGN = assign;
            this.LEFT = left;
            this.RIGHT = right;

            DEF.add(this.ASSIGN);
            if (this.LEFT.charAt(0) != '#')
                REF.add(this.LEFT);
            if (this.RIGHT.charAt(0) != '#')
                REF.add(this.RIGHT);
        }

        @Override
        public String toString() {
            String buffer = "";
            buffer += String.format("// Life_IN  : %s\n", Life_IN);
            buffer += String.format("// Life_OUT : %s\n", Life_OUT);
            buffer += String.format("// Next_IN  : %s\n", Next_IN);
            buffer += String.format("// Next_OUT : %s\n", Next_OUT);
            return buffer;
        }
    }
}
