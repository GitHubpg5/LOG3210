package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created: 19-01-10
 * Last Changed: 23-02-03
 * Author: Félix Brunet
 * <p>
 * Description: Ce visiteur explorer l'AST est renvois des erreurs lorsqu'une erreur sémantique est détectée.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    private HashMap<String, VarType> SymbolTable = new HashMap<>(); // mapping variable -> type

    // variable pour les metrics
    public int VAR = 0;
    public int WHILE = 0;
    public int IF = 0;
    public int ENUM_VALUES = 0;
    public int OP = 0;

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    IMPORTANT:
    *
    * L'implémentation des visiteurs se base sur la grammaire fournie (Grammaire.jjt). Il faut donc la consulter pour
    * déterminer les noeuds enfants à visiter. Cela vous sera utile pour lancer les erreurs au bon moment.
    * Pour chaque noeud, on peut :
    *   1. Déterminer le nombre d'enfants d'un noeud : jjtGetNumChildren()
    *   2. Visiter tous les noeuds enfants: childrenAccept()
    *   3. Accéder à un noeud enfant : jjtGetChild()
    *   4. Visiter un noeud enfant : jjtAccept()
    *   5. Accéder à m_value (type) ou m_ops (vecteur des opérateurs) selon la classe de noeud AST (src/analyser/ast)
    *
    * Cela permet d'analyser l'intégralité de l'arbre de syntaxe abstraite (AST) et d'effectuer une analyse sémantique du code.
    *
    * Le Visiteur doit lancer des erreurs lorsqu'une situation arrive.
    *
    * Pour vous aider, voici le code à utiliser pour lancer les erreurs :
    *
    * - Utilisation d'identifiant non défini :
    *   throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());
    *
    * - Plusieurs déclarations pour un identifiant. Ex : num a = 1; bool a = true; :
    *   throw new SemantiqueError(String.format("Identifier %s has multiple declarations", varName));
    *
    * - Utilisation d'un type num dans la condition d'un if ou d'un while :
    *   throw new SemantiqueError("Invalid type in condition");
    *
    * - Utilisation de types non valides pour des opérations de comparaison :
    *   throw new SemantiqueError("Invalid type in expression");
    *
    * - Assignation d'une valeur à une variable qui a déjà reçu une valeur d'un autre type. Ex : a = 1; a = true; :
    *   throw new SemantiqueError(String.format("Invalid type in assignation of Identifier %s", varName));
    *
    * - Le type de retour ne correspond pas au type de fonction :
    *   throw new SemantiqueError("Return type does not match function type");
    * */


    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, data);
        m_writer.print(String.format("{VAR:%d, WHILE:%d, IF:%d, ENUM_VALUES:%d, OP:%d}", this.VAR, this.WHILE, this.IF, this.ENUM_VALUES, this.OP));
        return null;
    }

    // Enregistre les variables avec leur type dans la table symbolique.
    @Override
    public Object visit(ASTDeclaration node, Object data) {

        if (node.jjtGetNumChildren() == 2){
            ASTIdentifier childNode1 = (ASTIdentifier) node.jjtGetChild(0);
            ASTIdentifier childNode2 = (ASTIdentifier) node.jjtGetChild(1);

            if(SymbolTable.containsKey(childNode2.getValue())) throw new SemantiqueError(String.format("Identifier %s has multiple declarations", childNode2.getValue()));
            else if (!SymbolTable.containsKey(childNode1.getValue()) || SymbolTable.get(childNode1.getValue()) != VarType.EnumType){
                throw new SemantiqueError(String.format("Identifier %s has been declared with the type %s that does not exist", childNode2.getValue(), childNode1.getValue()));
            }

            SymbolTable.put(childNode2.getValue(), VarType.EnumVar);
        }
        else {
            ASTIdentifier childNode = (ASTIdentifier) node.jjtGetChild(0);
            String varName = (childNode).getValue();
            if (SymbolTable.containsKey(varName)) {
                throw new SemantiqueError(String.format("Identifier %s has multiple declarations", varName));
            }
            if (Objects.equals(node.getValue(), "bool")) {
                SymbolTable.put(varName, VarType.Bool);
            } else if (Objects.equals(node.getValue(), "num")) {
                SymbolTable.put(varName, VarType.Number);
            } else if (Objects.equals(node.getValue(), "enum")) {
                SymbolTable.put(varName, VarType.EnumType);
            }
        }
        this.VAR++;
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    // Méthode qui pourrait être utile pour vérifier le type d'expression dans une condition.
    private void callChildrenCond(SimpleNode node) {
        DataStruct d = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, d);

        if (d.type != VarType.Bool) throw new SemantiqueError("Invalid type in condition");

        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
        }
    }

    // les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
    // On doit aussi compter les conditions dans les variables IF et WHILE
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        callChildrenCond(node);
        this.IF++;
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        callChildrenCond(node);
        this.WHILE++;
        return null;
    }

    // On doit vérifier que le type de la variable est compatible avec celui de l'expression.
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String varNameLeft = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        DataStruct rightChild = new DataStruct();
        node.jjtGetChild(1).jjtAccept(this, rightChild);
        if (SymbolTable.get(varNameLeft) == VarType.EnumVar && rightChild.type == VarType.EnumValue) return null;
        if (SymbolTable.get(varNameLeft) != rightChild.type) throw new SemantiqueError(String.format(
                "Invalid type in assignation of Identifier %s", varNameLeft));
        return null;
    }

    @Override
    public Object visit(ASTEnumStmt node, Object data) {
        DataStruct d = new DataStruct();
        d.type = VarType.EnumType;
        String typeName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if (SymbolTable.containsKey(typeName)){throw new SemantiqueError(String.format("Identifier %s has multiple declarations", typeName));}
        SymbolTable.put(typeName, VarType.EnumType);
        node.jjtGetChild(0).jjtAccept(this, d);
        int numChildren = node.jjtGetNumChildren();
        this.ENUM_VALUES += numChildren - 1;

        d.type = VarType.EnumValue;

        for(int i = 1; i < numChildren; i++){
            String CurrentSymbol = ((ASTIdentifier) node.jjtGetChild(i)).getValue();

            if(SymbolTable.containsKey(CurrentSymbol)){
                throw new SemantiqueError(String.format("Identifier %s has multiple declarations", CurrentSymbol));
            }
            SymbolTable.put(CurrentSymbol, VarType.EnumValue);
            node.jjtGetChild(i).jjtAccept(this, d);
        }
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        String child = ((ASTIdentifier)node.jjtGetChild(0)).getValue();
        if (SymbolTable.get(child) != VarType.Number && SymbolTable.get(child) != VarType.EnumVar) throw new SemantiqueError(String.format("Invalid type in switch of Identifier %s", ((ASTIdentifier)node.jjtGetChild(0)).getValue()));

        for(int i = 1; i < numChildren; i++){
            DataStruct d = new DataStruct();
            d.type = SymbolTable.get(child);
            node.jjtGetChild(i).jjtAccept(this, d);
        }
        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        DataStruct d = new DataStruct();
        Node childNode = node.jjtGetChild(0);
        node.jjtGetChild(0).jjtAccept(this, d);
        if (d.type == VarType.EnumValue) d.type = VarType.EnumVar;


        if(((DataStruct)data).type != d.type) {
            if(childNode instanceof ASTIdentifier) throw new SemantiqueError(
                    String.format("Invalid type in case of Identifier %s",
                            ((ASTIdentifier) node.jjtGetChild(0)).getValue()));
            else if (childNode instanceof ASTIntValue) throw new SemantiqueError(
                    String.format("Invalid type in case of integer %s",
                            ((ASTIntValue) node.jjtGetChild(0)).getValue()));
        }
        node.jjtGetChild(1).jjtAccept(this, d);
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        /*
            Attention, ce noeud est plus complexe que les autres :
            - S’il n'a qu'un seul enfant, le noeud a pour type le type de son enfant.
            - S’il a plus d'un enfant, alors il s'agit d'une comparaison. Il a donc pour type "Bool".
            - Il n'est pas acceptable de faire des comparaisons de booléen avec les opérateurs < > <= >=.
            - Les opérateurs == et != peuvent être utilisé pour les nombres et les booléens, mais il faut que le type
            soit le même des deux côtés de l'égalité/l'inégalité.
        */
        int numChildren = node.jjtGetNumChildren();

        if (numChildren == 1) node.childrenAccept(this, data);
        else if (numChildren > 1){
            node.jjtGetChild(0).jjtAccept(this, data);
            DataStruct dataRight = new DataStruct();
            node.jjtGetChild(1).jjtAccept(this, dataRight);
            if (((DataStruct)data).type != dataRight.type) throw new SemantiqueError("Invalid type in expression");

            String op = node.getValue();
            switch (dataRight.type) {
                case Bool:
                    if (!(op.equals("==") || op.equals("!="))) throw new SemantiqueError("Invalid type in expression");
                    break;
                case Number:
                    if (op.equals("&&") || op.equals("||") || op.equals("!")) throw new SemantiqueError("Invalid type in expression");
                    break;
            }
            ((DataStruct)data).type = VarType.Bool;
            this.OP++;
        }
        return null;
    }

    /*
        Opérateur binaire :
        - s’il n'y a qu'un enfant, aucune vérification à faire.
        - Par exemple, un AddExpr peut retourner le type "Bool" à condition de n'avoir qu'un seul enfant.
     */
    @Override
    public Object visit(ASTAddExpr node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
            this.OP++;
            if (numChildren > 1) {
                if(d.type == VarType.Bool) throw new SemantiqueError("Invalid type in expression");
            }
            if (d.type != null) ((DataStruct)data).type = d.type;
        }
        this.OP--;
        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
            this.OP++;
            if (numChildren > 1) {
                if(d.type == VarType.Bool) throw new SemantiqueError("Invalid type in expression");
            }
            if (d.type != null) ((DataStruct)data).type = d.type;
        }
        this.OP--;
        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
            this.OP++;
            if (numChildren > 1) {
                if(d.type == VarType.Number) throw new SemantiqueError("Invalid type in expression");
            }
            if (d.type != null) ((DataStruct)data).type = d.type;
        }
        this.OP--;
        return null;
    }

    /*
        Opérateur unaire
        Les opérateurs unaires ont toujours un seul enfant. Cependant, ASTNotExpr et ASTUnaExpr ont la fonction
        "getOps()" qui retourne un vecteur contenant l'image (représentation str) de chaque token associé au noeud.
        Il est utile de vérifier la longueur de ce vecteur pour savoir si un opérande est présent.
        - S’il n'y a pas d'opérande, ne rien faire.
        - S’il y a un (ou plus) opérande, il faut vérifier le type.
    */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        if (!node.getOps().isEmpty()){
            if (((DataStruct)data).type != VarType.Bool) throw new SemantiqueError("Invalid type in expression");
            this.OP++;
        }
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        node.jjtGetChild(0).jjtAccept(this, data);
        if (!node.getOps().isEmpty()){
            if (((DataStruct)data).type != VarType.Number) throw new SemantiqueError("Invalid type in expression");
            this.OP++;
        }
        return null;
    }

    /*
        Les noeud ASTIdentifier ayant comme parent "GenValue" doivent vérifier leur type.
        On peut envoyer une information à un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        ((DataStruct) data).type = VarType.Bool;
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {

        //if (node.jjtGetParent() instanceof ASTGenValue) {
            String varName = node.getValue();
            VarType varType = SymbolTable.get(varName);

            ((DataStruct) data).type = varType;


        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        ((DataStruct) data).type = VarType.Number;
        return null;
    }


    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number,
        EnumType,
        EnumVar,
        EnumValue
    }


    private class DataStruct {
        public VarType type;

        public DataStruct() {
        }

        public DataStruct(VarType p_type) {
            type = p_type;
        }

    }
}