import java.util.HashMap;
import java.util.Map;

public class Passenger extends Entity {
  private Passenger(String name) { super(name); }
  private static Map<String, Passenger> instances = new HashMap<>();
  public static Passenger make(String name) {
    if(instances.containsKey(name)) {
      return instances.get(name);
    }
    instances.put(name,new Passenger(name));
    return instances.get(name);

  }
}
