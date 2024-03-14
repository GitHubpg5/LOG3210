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
public class IntermediateCodeGenVisitor implements ParserVisitor {
    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();
    public HashMap<String, Integer> EnumValueTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenVisitor(PrintWriter writer) {
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
        VarType varType;

        if (node.getValue() == null) {
            varName = ((ASTIdentifier) node.jjtGetChild(1)).getValue();
            varType = VarType.EnumVar;
        } else
            varType = node.getValue().equals("num") ? VarType.Number : VarType.Bool;

        SymbolTable.put(varName, varType);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        //node.childrenAccept(this, data);
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if(numChildren == 1){
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else {
            int i = 0;
            while (i < numChildren -1){
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
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTBreakStmt node, Object data) {
        node.childrenAccept(this, data);
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
        //childrenAccept(this, data);
        int numChildren = node.jjtGetNumChildren();
        switch (numChildren){
            case (1): return node.jjtGetChild(0).jjtAccept(this, data);
            case (2):
                String ifLabel = newLabel();
                node.jjtGetChild(0).jjtAccept(this, new BoolLabel(ifLabel, (String)data));
                m_writer.println(ifLabel);
                node.jjtGetChild(1).jjtAccept(this, data);
                break;
            case (3):
                String trueLabel = newLabel();
                String falseLabel = newLabel();
                BoolLabel boolLabel = new BoolLabel(trueLabel, falseLabel);
                node.jjtGetChild(0).jjtAccept(this, boolLabel);
                m_writer.println(trueLabel);
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
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        // TODO
        if (SymbolTable.get(identifier) == VarType.Number) {
            m_writer.println(identifier + " = " + node.jjtGetChild(1).jjtAccept(this, data));
        }
        else {
            BoolLabel boolLabel = new BoolLabel(newLabel(), newLabel());
            node.jjtGetChild(1).jjtAccept(this, boolLabel);
            m_writer.println(boolLabel.lTrue);
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
        if(numChildren == 1 || ops.isEmpty()){
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else{
            String newId = newID();
            String gauche = (String)node.jjtGetChild(0).jjtAccept(this, data);
            String droite = (String)node.jjtGetChild(1).jjtAccept(this, data);
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
        if (nbOps > 0){
            idActuel = newID();
            m_writer.println(idActuel + " = - " + enfant);
            for (int i = 1; i < nbOps ; i++) {
                String idProchain = newID();
                m_writer.println(idProchain + " = - " + idActuel);
                idActuel = idProchain;
            }
            return idActuel;
        }
        else return enfant;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        // TODO

        int numChildren = node.jjtGetNumChildren();
        if(numChildren == 1){
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else{
            if(node.getOps().get(0).equals("&&")){
                String newLabel = newLabel();
                node.jjtGetChild(0).jjtAccept(this, new BoolLabel(newLabel, ((BoolLabel)data).lFalse));
                m_writer.println(newLabel);
                node.jjtGetChild(1).jjtAccept(this, data);
            }
            else {
                String newLabel = newLabel();
                node.jjtGetChild(0).jjtAccept(this, new BoolLabel(((BoolLabel)data).lTrue, newLabel));
                m_writer.println(newLabel);
                node.jjtGetChild(1).jjtAccept(this, data);
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if(numChildren == 1){
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        else {
            m_writer.println("if " + node.jjtGetChild(0).jjtAccept(this, data) + " " + node.getValue() + " "
                    + node.jjtGetChild(1).jjtAccept(this, data) + " goto " + ((BoolLabel)data).lTrue);
            m_writer.println("goto " + ((BoolLabel)data).lFalse);
        }
        return null;
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        // TODO
        if(!(node.getOps().size() % 2 == 0)) {
            return node.jjtGetChild(0).
                    jjtAccept(this, new BoolLabel(((BoolLabel)data).lFalse, ((BoolLabel)data).lTrue));
        }
        else return node.jjtGetChild(0).jjtAccept(this, data);
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
        m_writer.println("goto " + (node.getValue() ? ((BoolLabel)data).lTrue : ((BoolLabel)data).lFalse));
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        String val = node.getValue();
        if (SymbolTable.get(val) == VarType.Bool) {
            m_writer.println("if " + val + " == 1 goto " + ((BoolLabel)data).lTrue);
            m_writer.println("goto " + ((BoolLabel)data).lFalse);
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
