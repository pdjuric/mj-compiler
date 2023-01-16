package rs.ac.bg.etf.pp1.extended;


import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.LinkedList;
import java.util.List;

public class Tab extends rs.etf.pp1.symboltable.Tab {

    private static List<Obj> classObjs = null;
    public static final Struct boolType = new Struct(Struct.Bool);

    public static Obj __temp0, __temp1;
    public static void init() {
        rs.etf.pp1.symboltable.Tab.init();
        classObjs = new LinkedList<>();
        currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType, 0, 0));
    }

    public static String findClassName(Struct classStruct) {
        Obj classObj = classObjs.stream().filter(c -> c.getType() == classStruct).findFirst().orElse(null);
        return (classObj == null) ?  "[unknown class]" : classObj.getName();
    }

    public static Obj insert(int kind, String name, Struct type) {
        Obj newObj = rs.etf.pp1.symboltable.Tab.insert(kind, name, type);
        if (newObj.getKind() == Obj.Type && newObj.getType().getKind() == Struct.Class)
            classObjs.add(newObj);
        return newObj;
    }

    public static void insertConstructor(Obj c) {
        currentScope.addToLocals(c);
    }

    public static Obj insert(int kind, String name, Struct type, int adr, int level) {
        Obj obj = Tab.insert(kind, name, type);
        obj.setLevel(level);
        obj.setAdr(adr);
        return obj;
    }

    public static Obj insertCopy(Obj obj) {
        return Tab.insert(obj.getKind(), obj.getName(), obj.getType(), obj.getAdr(), obj.getLevel());
    }

    public static List<Obj> getClassObjs() {
        return classObjs;
    }

}
