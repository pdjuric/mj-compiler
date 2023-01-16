package rs.ac.bg.etf.pp1.method_declaration;

import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.ac.bg.etf.pp1.extended.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static rs.ac.bg.etf.pp1.extended.Tab.insertConstructor;


public class ClassConstructor extends ClassMethod {

    private final String className;

    /**
     * List of iterators over parameters in all constructors.
     */
    private List<Iterator<Obj>> iters;

    /**
     * Whether an error occurred while parsing the constructor.
     */
    private boolean errorOccurred = false;

    /**
     * List of all declared constructors in the class.
     */
    private final LinkedList<Obj> declaredConstructors;

    /**
     * Creates a new constructor.
     * @param ctor constructor object
     * @param declaredConstructors list of all declared constructors in the class
     */
    public ClassConstructor(Obj ctor, LinkedList<Obj> declaredConstructors) {
        super(ctor, ctor.getType());
        this.declaredConstructors = declaredConstructors;
        className = Tab.findClassName(ctor.getType());

        iters = new ArrayList<>(declaredConstructors.size());
        for (Obj declaredConstructor : declaredConstructors)
            iters.add(declaredConstructor.getLocalSymbols().iterator());

        // skip 'this'
        for (Iterator<Obj> it : iters) it.next();
    }

    /**
     * Adds a parameter to the constructor.
     * Should be called before {@link #endPar()} and {@link #endMethodDeclaration()}. (unchecked)
     * @param par parameter to add
     * @return {@link Status#Ok()}
     */
    public Status addPar(Obj par) {
        super.addPar(par);

        iters = iters
                .stream()
                .filter(iter -> {
                    if (!iter.hasNext()) return false;
                    Obj nextPar = iter.next();
                    return nextPar.getFpPos() != 0 && nextPar.getType().equals(par.getType());
                })
                .collect(Collectors.toList());

        return Status.Ok();
    }

    /**
     * Ends the parameter list.
     * Should be called before {@link #endMethodDeclaration()}. (unchecked)
     * @return {@link Status#DuplicateConstructor(String)} if there's a constructor with the same parameters,
     *         {@link Status#Ok()} otherwise
     */
    public Status endPar() {
        super.endPar();

        // check whether there's a constructor with the same parameters
        for (Iterator<Obj> iter : iters)
            if (!iter.hasNext() || iter.next().getFpPos() == 0) {
                errorOccurred = true;
                return Status.DuplicateConstructor(className);
            }

        return Status.Ok();
    }

    /**
     * Ends the constructor declaration.
     * @return {@link Status#Ok()}
     */
    public Status endMethodDeclaration() {
        super.endMethodDeclaration();
        if (!errorOccurred) {
            insertConstructor(obj);
            declaredConstructors.add(obj);
        }
        return Status.Ok();
    }


}
