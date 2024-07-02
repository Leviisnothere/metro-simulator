import java.util.HashMap;
import java.util.Map;

public class Train extends Entity {

  private static Map<String, Train> instances = new HashMap<>();
  private Train(String name) { super(name); }

  public static Train make(String name) {
    if(instances.containsKey(name)) {
      return instances.get(name);
    }
    instances.put(name,new Train(name));
    return instances.get(name);
  }
}
