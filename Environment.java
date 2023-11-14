import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;

class Environment {
    private HashMap<String,Boolean> variableValues = new HashMap<String,Boolean>();
    public Environment() { }
    public void setVariable(String name, Boolean value) {
	variableValues.put(name, value);
    }
    
    public Boolean getVariable(String name){
	Boolean value = variableValues.get(name); 
	if (value == null) { System.err.println("Variable not defined: "+name); System.exit(-1); }
	return value;
    }

    public Boolean hasVariable(String name){
	Boolean v = variableValues.get(name); 
	return (v != null);	
    }
    
    public String toString() {
	String table = "";
	for (Entry<String,Boolean> entry : variableValues.entrySet()) {
	    table += entry.getKey() + "\t-> " + entry.getValue() + "\n";
	}
	return table;
    }

	public List<String> getVariableNames(){
		List<String> names = new ArrayList<String>();
		for(Entry<String,Boolean> entry : variableValues.entrySet()){
			names.add(entry.getKey());
		}
		return names;
	}
}

