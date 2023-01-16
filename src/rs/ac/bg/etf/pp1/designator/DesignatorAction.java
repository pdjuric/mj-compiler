package rs.ac.bg.etf.pp1.designator;

import rs.ac.bg.etf.pp1.ast.*;

public class DesignatorAction {

    public static final int GET = 0;
    public static final int DEFERRED_SET = 1;
    public static final int CALL = 2;

    private static String getName(Class<?> clazz) {
        String qName = clazz.getName();
        return  qName.substring(qName.lastIndexOf('.') + 1);
    }

    public static int getAction(SyntaxNode curr) {
        Class<?> parentClass = curr.getParent().getClass();
        String key = getName(parentClass);

        switch (key) {
            case "ReadStmt":
            case "Assignment":
            case "Increment":
            case "Decrement":
            case "NotNullDesignator":
                return DEFERRED_SET;

            case "ForEachStmtDesignator":
            case "ArrayAssignmentStatement":
            case "DesignatorFactor":
            case "MemberAccessDesignator":
            case "IndexingDesignator":
                return GET;

            case "CallingFuncDesignator":
                return CALL;

            default:
                throw new RuntimeException(key);
        }
    }

}
