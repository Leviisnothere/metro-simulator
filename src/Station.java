import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Station extends Entity {
  JTextField jTextField;
  Color color;
  private Station(String name,JTextField jTextField) {
    super(name);
    this.jTextField = jTextField;
    this.color = jTextField.getBackground();
  }
  private static Map<String, Station> instances = new HashMap<>();
  public static Station make(String name) {
    return instances.get(name);
  }
  public static Station make(String name, JTextField jTextField){
    if(instances.containsKey(name)) {
      return instances.get(name);
    }
    instances.put(name,new Station(name, jTextField));
    return instances.get(name);
  }

}
