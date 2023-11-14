import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;
public abstract class AST{
    public void error(String msg){
	System.err.println(msg);
	System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and
   Not (Negation) */

abstract class Expr extends AST{
    abstract boolean eval(Environment environment);
    abstract List<String> getSignalNames();
}

class Conjunction extends Expr{
    Expr e1,e2;
    Conjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}
    public boolean eval(Environment environment){
        return ((e1.eval(environment)) && (e2.eval(environment)));
    }

    public List<String> getSignalNames(){
        List<String> sigNames = e1.getSignalNames();
        sigNames.addAll(e2.getSignalNames());
        return sigNames;
    }

}

class Disjunction extends Expr{
    Expr e1,e2;
    Disjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}

    public boolean eval(Environment environment){
        return ((e1.eval(environment)) || (e2.eval(environment)));
    }
    public List<String> getSignalNames(){
        List<String> sigNames = e1.getSignalNames();
        sigNames.addAll(e2.getSignalNames());
        return sigNames;
    }
}

class Negation extends Expr{
    Expr e;
    Negation(Expr e){this.e=e;}
    public boolean eval(Environment environment){
        return (!e.eval(environment));
    }
    public List<String> getSignalNames(){
        return e.getSignalNames();
    }
}

class Signal extends Expr{
    String varname; // a signal is just identified by a name
    Signal(String varname){this.varname=varname;}
    String getVarname(){
        return varname;
    }

    public boolean eval(Environment environment){
        return environment.getVariable(varname);
        
    }
    public List<String> getSignalNames(){
        List<String> sigName = new ArrayList();
        sigName.add(varname);
        return sigName;
    }
}

// Latches have an input and output signal

class Latch extends AST{
    String inputname;
    String outputname;
    Latch(String inputname, String outputname){
	this.inputname=inputname;
	this.outputname=outputname;
    }
    public void initialize(Environment environment){
        environment.setVariable(outputname, false);
    }

    public void nextCycle(Environment environment){
        environment.setVariable(outputname, environment.getVariable(inputname));
    }
}

// An Update is any of the lines " signal = expression "
// in the .update section

class Update extends AST{
    String name;
    Expr e;
    Update(String name, Expr e){this.e=e; this.name=name;}

    public void eval(Environment environment){
        environment.setVariable(name, e.eval(environment));
    }

    public List<String> getExpressionSigNames(){
        return e.getSignalNames();
    }
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST{
    String signal;
    Boolean[] values;
    Trace(String signal, Boolean[] values){
	this.signal=signal;
	this.values=values;
    }

    public String toString(){
        String stringValues = "";
        for (Boolean b: values){
            if (b){
                stringValues += "1";
            } else {
                stringValues += "0";
            }
        }
        return stringValues;
    }
}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, and updates. Additionally for each
   input signal, it has a Trace as simulation input.

   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use them for assignment 2 to
   implement the interpreter:

   1. to have simlength as the length of the traces in siminputs. (The
   simulator should check they have all the same length and stop with
   an error otherwise.) Now simlength is the number of simulation
   cycles the interpreter should run.

   2. to store in simoutputs the value of the output signals in each
   simulation cycle, so they can be displayed at the end. These traces
   should also finally have the length simlength.
*/

class Circuit extends AST{
    String name;
    List<String> inputs;
    List<String> outputs;
    List<Latch>  latches;
    List<Update> updates;
    List<Trace>  siminputs;
    List<Trace>  simoutputs;
    int simlength;
    Circuit(String name,
	    List<String> inputs,
	    List<String> outputs,
	    List<Latch>  latches,
	    List<Update> updates,
	    List<Trace>  siminputs){
	this.name=name;
	this.inputs=inputs;
	this.outputs=outputs;
	this.latches=latches;
	this.updates=updates;
	this.siminputs=siminputs;
    this.simlength=0;
    }

    public void runSimulator(Environment environment){
        initialize(environment);
        validateUpdates();
        for (int i = 0; i < simlength; i++){
            nextCycle(environment, i);

            validateSignals(environment);
        }
        printOutputs();
    }

    public void initialize(Environment environment){
        if(siminputs.size() == 0){
            error("siminput was empty");
        }
        simlength = siminputs.get(0).values.length;

        if(simlength == 0){
            error("No values for siminput");
        }


        for (Trace t : siminputs) {

            if (t.values[0] == null){
                error("siminput 0-th value equals null");
            }
            if(t.values.length != simlength){
                error("siminputs not same length!");
            }


            environment.setVariable(t.signal, t.values[0]);
        }
        for (Latch l : latches) {
            l.initialize(environment);
        }
        for (Update u : updates) {
            u.eval(environment);
        }
        simoutputs = new ArrayList<Trace>();
        for(String s : outputs ){
            simoutputs.add(new Trace(s, new Boolean[simlength]));
        }
        //System.out.println(environment.toString());
    }
    public void nextCycle(Environment environment, int i){
        if (siminputs.size() == 0){
            error("siminput was empty");
        }
        for (Trace t : siminputs) {
            if (t.values[i] == null){
                error("siminput i-th value equals null");
            }
            environment.setVariable(t.signal, t.values[i]);
        }
        for (Latch l : latches) {
            l.nextCycle(environment);
        }
        for (Update u : updates) {
            u.eval(environment);
        }
        for(String s : outputs){
            //System.out.println(environment.getVariable(s));
            Trace t = simoutputs.stream().filter(a -> a.signal.equals(s)).findFirst().get();
            t.values[i] = environment.getVariable(s);

        }

        //System.out.println(environment.toString());
    }

    private void validateSignals(Environment environment){


        List<String> actualSignals = new ArrayList<String>();
        actualSignals.addAll(inputs);
        /*
        for (Trace t: siminputs) {
            actualSignals.add(t.signal);
        }

         */
        for (Latch l: latches) {
            actualSignals.add(l.outputname);
        }
        for (Update u: updates) {
            actualSignals.add(u.name);
        }

        for(String regSig : environment.getVariableNames()){
            if(!actualSignals.contains(regSig)){
                error("Invalid signal \'" + regSig + "\' Unused");
            }
        }


        List<String> tempSignals = new ArrayList<String>();
        for(String sig : actualSignals){
            if(tempSignals.contains(sig)){
                error("Invalid signal \'" + sig + "\' Multiple occurences");
            }
            tempSignals.add(sig);
        }



        /*
        String[] inputArray = inputs.toArray(new String[0]);
        String[] updatesArray = new String[updates.size()];
        String[] latchesArray = new String[latches.size()];

        for(int i = 0; i < updates.size(); i++){
            updatesArray[i] = updates.get(i).name;
        }
        for(int i = 0; i < latches.size(); i++){
            String name = latches.get(i).outputname;
        }

        for(Trace sinput : siminputs){
            int inpFreq = Collections.frequency(Arrays.asList(inputArray), sinput.signal);
            int upFreq = Collections.frequency(Arrays.asList(updatesArray), sinput.signal);
            int latFreq = Collections.frequency(Arrays.asList(latchesArray), sinput.signal);

            System.out.println("====" + sinput.signal + "======");
            System.out.println(inpFreq);
            System.out.println(upFreq);
            System.out.println(latFreq);
            System.out.println("====");

                                                    
            if( (inpFreq + upFreq + latFreq) != 1){
                error("Invalid signal!! " + sinput.signal);
            }

        }

         */


    }

    public void validateUpdates(){
        List<String> previousOutputs = new ArrayList<String>();
        List<String> latchOutputs = new ArrayList<String>();
        for(Latch l : latches){
            latchOutputs.add(l.outputname);
        }
        for(Update update : updates){
            for(String sigName : update.getExpressionSigNames()){
                boolean inInputs = inputs.contains(sigName);
                boolean inLatchOutputs = latchOutputs.contains(sigName);
                boolean inPrevUpdate = previousOutputs.contains(sigName);

                if(!inInputs && !inLatchOutputs && !inPrevUpdate){
                    error("Cyclic expresion in update! > " + sigName);
                }
            }
            previousOutputs.add(update.name);
        }

    }



    public void printOutputs(){
        for(Trace out : simoutputs){
            System.out.println(out.toString() + " " + ": <b>" + out.signal + "</b>");
            System.out.println("<br>");
        }
    }
}
