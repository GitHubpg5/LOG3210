package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;


/**
 * Ce visiteur explore l'AST et génère du code intermédiaire.
 *
 * @author Félix Brunet
 * @author Doriane Olewicki
 * @author Quentin Guidée
 * @author Raphaël Tremblay
 * @version 2024.02.26
 */
public class IntermediateCodeGenFallVisitor implements ParserVisitor {
    public static final String FALL = "fall";

    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();
    public HashMap<String, Integer> EnumValueTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenFallVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    private String newID() {
        return "_t" + id++;
    }

    private String newLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        String label = newLabel();
        node.childrenAccept(this, label);
        // TODO
        m_writer.println(label);
        return null;
    }

    @Override
    public Object visit(ASTDeclaration node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        IntermediateCodeGenFallVisitor.VarType varType;

        if (node.getValue() == null) {
            varName = ((ASTIdentifier) node.jjtGetChild(1)).getValue();
            varType = IntermediateCodeGenFallVisitor.VarType.EnumVar;
        } else
            varType = node.getValue().equals("num") ? IntermediateCodeGenFallVisitor.VarType.Number : IntermediateCodeGenFallVisitor.VarType.Bool;

        SymbolTable.put(varName, varType);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 0) return null;
        else if (numChildren == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            int i = 0;
            while (i < numChildren - 1) {
                String label = newLabel();
                node.jjtGetChild(i).jjtAccept(this, label);
                m_writer.println(label);
                i++;
            }
            node.jjtGetChild(i).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTEnumStmt node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            String enumName = ((ASTIdentifier) node.jjtGetChild(i)).getValue();
            SymbolTable.put(enumName, IntermediateCodeGenFallVisitor.VarType.EnumType);
            EnumValueTable.put(enumName, i - 1);
        }
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        //node.childrenAccept(this, data);
        // TODO
        int numChildren = node.jjtGetNumChildren();
        String switchFollow = (String) data;
        String identifier = (String) node.jjtGetChild(0).jjtAccept(this, data);
        // String topLabel = newLabel();
        Vector<String> labels = new Vector<String>();
        labels.add(switchFollow);
        for (int i = 1; i < numChildren - 1; i++) {
            String value = (String) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, labels);
            labels.add(newLabel());
            m_writer.println("if " + identifier + " == " + EnumValueTable.get(value) + " goto " + labels.get(labels.size() - 1));
            labels.add(newLabel());
            m_writer.println("goto " + labels.get(labels.size() - 1));
            for (int j = 0; j < labels.size() - 1; j++) {
                m_writer.println(labels.remove(labels.size() - 2));
            }
            node.jjtGetChild(i).jjtAccept(this, labels);
        }
        String value = (String) node.jjtGetChild(numChildren - 1).jjtGetChild(0).jjtAccept(this, labels);
        labels.add(newLabel());
        m_writer.println("if " + identifier + " == " + EnumValueTable.get(value) + " goto " + labels.get(labels.size() - 1));
        m_writer.println("goto " + labels.get(0));
        int var = labels.size();
        for (int j = 0; j < var - 1; j++) {
            m_writer.println(labels.remove(labels.size() - 1));
        }
        node.jjtGetChild(numChildren - 1).jjtAccept(this, labels);
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        // node.childrenAccept(this, data);
        // TODO

        // m_writer.println("if " + identifier + " != " + value + " goto " + data);
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 0) return null;
        for (int i = 1; i < numChildren; i++) {
            node.jjtGetChild(i).jjtAccept(this, ((Vector<String>) data).firstElement());
        }
        String lastChildClass = node.jjtGetChild(node.jjtGetNumChildren() - 1).getClass().toString();
        if (!lastChildClass.equals("class analyzer.ast.ASTBreakStmt") && ((Vector<String>) data).size() > 1) {
            ((Vector<String>) data).add(newLabel());
            m_writer.println("goto " + ((Vector<String>) data).get(((Vector<String>) data).size() - 1));
        }
        if (((Vector<String>) data).size() > 1)
            m_writer.println(((Vector<String>) data).remove(1));
        return null;
    }

    @Override
    public Object visit(ASTBreakStmt node, Object data) {
        node.childrenAccept(this, data);
        m_writer.println("goto " + data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);

        return null;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        switch (numChildren) {
            case (1):
                return node.jjtGetChild(0).jjtAccept(this, data);
            case (2):
                node.jjtGetChild(0).jjtAccept(this, new IntermediateCodeGenFallVisitor.BoolLabel(FALL, (String) data));
                node.jjtGetChild(1).jjtAccept(this, data);
                break;
            case (3):
                String falseLabel = newLabel();
                IntermediateCodeGenFallVisitor.BoolLabel boolLabel = new IntermediateCodeGenFallVisitor.BoolLabel(FALL, falseLabel);
                node.jjtGetChild(0).jjtAccept(this, boolLabel);
                node.jjtGetChild(1).jjtAccept(this, data);
                m_writer.println("goto " + data);
                m_writer.println(falseLabel);
                node.jjtGetChild(2).jjtAccept(this, data);
                break;
        }
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        String begin = newLabel();
        m_writer.println(begin);
        node.jjtGetChild(0).jjtAccept(this, new IntermediateCodeGenFallVisitor.BoolLabel(FALL, (String) data));
        node.jjtGetChild(1).jjtAccept(this, begin);
        m_writer.println("goto " + begin);

        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
        // TODO

        String topLabel = newLabel();
        String incLabel = newLabel();
        String condLabel = newLabel();
        node.jjtGetChild(0).jjtAccept(this, topLabel);
        m_writer.println(topLabel + "TOP");
        node.jjtGetChild(1).jjtAccept(this, new IntermediateCodeGenFallVisitor.BoolLabel(condLabel, (String) data));
        m_writer.println(condLabel + "MID");
        node.jjtGetChild(3).jjtAccept(this, incLabel);
        m_writer.println(incLabel + "MID 2");
        node.jjtGetChild(2).jjtAccept(this, topLabel);
        m_writer.println("goto " + topLabel);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        // TODO
        if (SymbolTable.get(identifier) == IntermediateCodeGenFallVisitor.VarType.Number) {
            m_writer.println(identifier + " = " + node.jjtGetChild(1).jjtAccept(this, data));
        } else if (SymbolTable.get(identifier) == IntermediateCodeGenFallVisitor.VarType.EnumVar) {
            String value = (String) node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.println(identifier + " = " + EnumValueTable.get(value));
        } else {
            // String firstLabel =
            String secondLabel = newLabel();
            IntermediateCodeGenFallVisitor.BoolLabel boolLabel = new IntermediateCodeGenFallVisitor.BoolLabel(FALL, secondLabel);
            node.jjtGetChild(1).jjtAccept(this, boolLabel);
            m_writer.println(identifier + " = 1");
            m_writer.println("goto " + data);
            m_writer.println(boolLabel.lFalse);
            m_writer.println(identifier + " = 0");
        }
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    public Object codeExtAddMul(SimpleNode node, Object data, Vector<String> ops) {
        // À noter qu'il n'est pas nécessaire de boucler sur tous les enfants.
        // La grammaire n'accepte plus que 2 enfants maximum pour certaines opérations, au lieu de plusieurs
        // dans les TPs précédents. Vous pouvez vérifier au cas par cas dans le fichier Grammaire.jjt.
        // TODO

        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1 || ops.isEmpty()) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String newId = newID();
            String gauche = (String) node.jjtGetChild(0).jjtAccept(this, data);
            String droite = (String) node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.println(newId + " = " + gauche + " " + ops.get(0) + " " + droite);
            return newId;
        }
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        // TODO
        String idActuel = " ";
        int nbOps = node.getOps().size();
        Object enfant = node.jjtGetChild(0).jjtAccept(this, data);
        if (nbOps > 0) {
            idActuel = newID();
            m_writer.println(idActuel + " = - " + enfant);
            for (int i = 1; i < nbOps; i++) {
                String idProchain = newID();
                m_writer.println(idProchain + " = - " + idActuel);
                idActuel = idProchain;
            }
            return idActuel;
        } else return enfant;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        String resp;
        if (numChildren == 1) {
            resp = (String) node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            if (node.getOps().get(0).equals("&&")) {

                BoolLabel boolLabelGauche = null;

                if (((BoolLabel) data).lFalse == FALL) {
                    boolLabelGauche = new BoolLabel(FALL, newLabel());
                } else {
                    boolLabelGauche = new BoolLabel(FALL, ((BoolLabel) data).lFalse);
                }
                resp = (String) node.jjtGetChild(0).jjtAccept(this, boolLabelGauche);
                node.jjtGetChild(1).jjtAccept(this, data);

                if (((BoolLabel) data).lFalse == FALL) {
                    m_writer.println(boolLabelGauche.lFalse);
                }
            } else {
                BoolLabel boolLabelGauche = null;
                if (((BoolLabel) data).lTrue == FALL) {
                    boolLabelGauche = new BoolLabel(newLabel(), FALL);
                } else boolLabelGauche = new BoolLabel(((BoolLabel) data).lTrue, FALL);

                resp = (String) node.jjtGetChild(0).jjtAccept(this, boolLabelGauche);
                node.jjtGetChild(1).jjtAccept(this, data);

                if (((BoolLabel) data).lTrue == FALL) {
                    m_writer.println(boolLabelGauche.lTrue);
                }
                /* String newLabel = newLabel();
                node.jjtGetChild(0).jjtAccept(this, new IntermediateCodeGenFallVisitor.BoolLabel(((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue, newLabel));
                m_writer.println(newLabel + "BOOL 2");
                resp = (String) node.jjtGetChild(1).jjtAccept(this, data);*/
            }
        }
        return resp;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String leftChild = (String) node.jjtGetChild(0).jjtAccept(this, data);
            String rightChild = (String) node.jjtGetChild(1).jjtAccept(this, data);
            String operator = node.getValue();
            if (((BoolLabel) data).lTrue != FALL && ((BoolLabel) data).lFalse != FALL) {
                m_writer.println("if " + leftChild + " " + operator + " " + rightChild + " goto " +
                        ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue);
                m_writer.println("goto " + ((IntermediateCodeGenFallVisitor.BoolLabel) data).lFalse);
            } else if (((BoolLabel) data).lTrue != FALL && ((BoolLabel) data).lFalse == FALL) {
                m_writer.println("if " + leftChild + " " + operator + " " + rightChild + " goto " +
                        ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue);
            } else if (((BoolLabel) data).lTrue == FALL && ((BoolLabel) data).lFalse != FALL) {
                m_writer.println("ifFalse " + leftChild + " " + operator + " " + rightChild + " goto " +
                        ((IntermediateCodeGenFallVisitor.BoolLabel) data).lFalse);
            } else throw new Error();
        }
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        // TODO
        if (!(node.getOps().size() % 2 == 0)) {
            return node.jjtGetChild(0).
                    jjtAccept(this, new IntermediateCodeGenFallVisitor.BoolLabel(((IntermediateCodeGenFallVisitor.BoolLabel) data).lFalse, ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue));
        } else return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
        // TODO
        //return null;
    }

    @Override
    public Object visit(ASTBoolValue node, Object data) {
        // TODO
        if (((BoolLabel) data).lTrue != FALL && node.getValue()) {
            m_writer.println("goto " + ((BoolLabel) data).lTrue);
        } else if (((BoolLabel) data).lFalse != FALL && !node.getValue()) {
            m_writer.println("goto " + ((BoolLabel) data).lFalse);
        }

        // m_writer.println("goto LOL" + (node.getValue() ? ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue : ((IntermediateCodeGenFallVisitor.BoolLabel) data).lFalse));
        return (node.getValue() ? ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue : ((IntermediateCodeGenFallVisitor.BoolLabel) data).lFalse);
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        String val = node.getValue();
        if (SymbolTable.get(val) == IntermediateCodeGenFallVisitor.VarType.Bool) {
            if (((BoolLabel) data).lTrue != FALL && ((BoolLabel) data).lFalse != FALL) {
                m_writer.println("if " + val + " == 1 goto " + ((BoolLabel) data).lTrue);
                m_writer.println("goto " + ((BoolLabel) data).lFalse);
            } else if (((BoolLabel) data).lTrue != FALL && ((BoolLabel) data).lFalse == FALL)
                m_writer.println("if " + val + " == 1 goto " + ((BoolLabel) data).lTrue);
            else if (((BoolLabel) data).lTrue == FALL && ((BoolLabel) data).lFalse != FALL)
                m_writer.println("ifFalse " + val + " == 1 goto " + ((BoolLabel) data).lFalse);
            else
                throw new Error();
        }
        return val;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }

    public enum VarType {
        Bool,
        Number,
        EnumType,
        EnumVar,
        EnumValue
    }

    private static class BoolLabel {
        public String lTrue;
        public String lFalse;

        public BoolLabel(String lTrue, String lFalse) {
            this.lTrue = lTrue;
            this.lFalse = lFalse;
        }
    }
}
