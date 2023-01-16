package rs.ac.bg.etf.pp1.function_call;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.ac.bg.etf.pp1.extended.Helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static rs.ac.bg.etf.pp1.error_reporting.Status.*;
import static rs.ac.bg.etf.pp1.extended.Helper.*;

public class ConstructorCall extends FunctionCallHelper {

    static class Candidate {
        Obj methodObj;
        Iterator<Obj> parIter;
        int parCnt;
        List<Integer> parMatchLevels = new LinkedList<>();

        Candidate(Obj method) {
            this.methodObj = method;
            parIter = method.getLocalSymbols().iterator();
            parIter.next();
            parCnt = method.getLevel() - 1;
        }
    }

    List<Candidate> candidates = new LinkedList<>();
    private int currParIdx = 0;
    private Obj matched = null;
    private int classParCnt = 0;
    private final String className;


    public ConstructorCall(Struct classStruct, String className) {
        super();
        this.className = className;
        Collection<Obj> symbols;
        if (classStruct.getMembersTable().symbols().isEmpty()) {
            symbols = Tab.currentScope.getOuter().getLocals().symbols();
        } else {
            symbols = classStruct.getMembersTable().symbols();
        }
        symbols.stream()
                .filter(Helper::isConstructor)
                .forEach(m -> candidates.add(new Candidate(m)));
    }


    public Status addArg(Struct argType) {
        List<Candidate> newCandidates = new LinkedList<>();
        for (Candidate c: candidates) if (c.parCnt > currParIdx) {
            Struct parType = c.parIter.next().getType();

            if (isAssignableTo(argType, parType)) {
                Integer lvl = getInheritanceLevel(argType, parType);
                if (lvl != null) {
                    c.parMatchLevels.add(lvl);
                    classParCnt++;
                }

                newCandidates.add(c);
            }
        }

        currParIdx++;
        this.candidates = newCandidates;
        return Ok();
    }

    public Status argEnd() {
        List<Candidate> candidateList = candidates.stream().filter(c -> c.parCnt == currParIdx).collect(Collectors.toList());
        if (candidateList.isEmpty()) return NoMatchingConstructor(className);

        List<Candidate> list = new LinkedList<>(candidateList);
        for (int i = 0; i < classParCnt; i++) {
            List<Candidate> currList = new LinkedList<>();
            Integer currMin = null;

            for (Candidate c: candidateList) {

                if (currMin == null || currMin > c.parMatchLevels.get(i)) {
                    currMin = c.parMatchLevels.get(i);
                    currList.clear();
                }

                if (c.parMatchLevels.get(i).equals(currMin))
                    if (list.contains(c))
                        currList.add(c);
                    else
                        // nije pripadao najspecificnijem tipu za prosle parametre
                        return AmbiguousConstructorCall(className);
            }

            list = currList;
        }

        if (list.size() > 1) throw new RuntimeException("Multiple Constructors in final list");
        matched = list.get(0).methodObj;
        return Ok();
    }

    public Obj getMethod() {
        return matched;
    }


}
