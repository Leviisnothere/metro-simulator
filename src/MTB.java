import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MTB {



  //Map<Station, Set<Passenger>> stationPassenger;
  Map<Station,Train> stationTrain;
  Map<Train, List<Station>> trainsStops;
  Map<Train, Boolean> trainsDirection; // true = forward, false = backward
  Map<Train, Set<Passenger>> trainPassenger;
  Map<Train, Station> trainsPos;
  Map<Passenger, List<Station>> passengerTrips;
  Map<Passenger, Station> passengerPos;
  Map<String, String> passengerMetroCard;
  JPanel MTAPanel;

  public MTB(JPanel MTAPanel) {
    trainsStops = new HashMap<>();
    trainsDirection = new HashMap<>();
    trainPassenger = new HashMap<>();
    trainsPos = new HashMap<>();
    passengerTrips = new HashMap<>();
    passengerPos = new HashMap<>();
    stationTrain = new HashMap<>();
    this.MTAPanel = MTAPanel;
    this.MTAPanel.setLayout(null);


  }

  // Adds a new transit line with given name and stations
  public void addLine(String name, List<String> stations, List<List<Integer>> positions) {
    List<Station> lineStations = new ArrayList<Station>();
    for(int i = 0; i < stations.size()-1; i++){

      List<Integer> stationAPos = positions.get(i);
      List<Integer> stationBPos = positions.get(i+1);

      String stationA_name = stations.get(i);
      String stationB_name = stations.get(i+1);

      JTextField stationFieldA = createStationField(name, stationA_name,stationAPos);
      JTextField stationFieldB = createStationField(name, stationB_name,stationBPos);

      Station stationA = Station.make(stationA_name, stationFieldA);
      Station stationB = Station.make(stationB_name, stationFieldB);
      this.MTAPanel.add(stationFieldA);
      this.MTAPanel.add(stationFieldB);

      LineComponent lineComponent = new LineComponent(stationFieldA, stationFieldB);
      this.MTAPanel.add(lineComponent);


      lineStations.add(stationA);
      if(i == stations.size() - 2)
        lineStations.add(stationB);
      stationTrain.putIfAbsent(stationA,null);
      stationTrain.putIfAbsent(stationB,null);

    }
    stationTrain.put(Station.make(stations.get(0)),Train.make(name));
    trainsPos.put(Train.make(name),Station.make(stations.get(0)));
    trainsStops.put(Train.make(name),lineStations);
    trainsDirection.put(Train.make(name),true);
    trainPassenger.put(Train.make(name),new HashSet<Passenger>());

  }

  // Adds a new planned journey to the simulation
  public void addJourney(String name, List<String> stations) {
    List<Station> tripStations = new ArrayList<Station>();
    for(String station: stations){
      tripStations.add(Station.make(station));
    }
    Passenger p = Passenger.make(name);
    passengerPos.put(p, Station.make(stations.get(0)));
    passengerTrips.put(p, tripStations);

    //stationPassenger.get(Station.make(stations.get(0))).add(p);
  }

  public void checkStart() {
    for(Passenger passenger: passengerPos.keySet()){
      if(!passengerPos.get(passenger).equals(passengerTrips.get(passenger).get(0)))
        throw new InvalidEventException("Exception caught in MBTA#checkStart");
    }
    for(Train train: trainsPos.keySet()){
      if(!trainsPos.get(train).equals(trainsStops.get(train).get(0)))
        throw new InvalidEventException("Exception caught in MBTA#checkStart");
    }
  }


  public void checkEnd() {
    for(Passenger passenger: passengerPos.keySet()){
      if(!passengerPos.get(passenger).equals(passengerTrips.get(passenger).get(passengerTrips.get(passenger).size()-1)))
        throw new InvalidEventException("Exception caught in MBTA#checkEnd");
    }
  }


  // reset to an empty simulation
  public void reset() {
    trainsStops.clear();
    passengerTrips.clear();
    trainsPos.clear();
    passengerPos.clear();
    trainsDirection.clear();
  }
  static class Config{
    public Map<String,List<String>> lines;
    public Map<String,List<List<Integer>>> positions;
    public Map<String,List<String>> trips;
    public Map<String,String> metroCard;
  }

  // adds simulation configuration from a file
  public void loadConfig(String filename) {
    this.reset();

    try{
      Gson gson = new Gson();
      Config config = gson.fromJson(new FileReader(filename), Config.class);
      if(config == null){
        System.out.println("null");
      }
      for(Map.Entry<String, List<String>> entry : config.lines.entrySet()){
        List<List<Integer>> positionList = config.positions.get(entry.getKey());
        addLine(entry.getKey(),entry.getValue(),positionList);
      }
      for(Map.Entry<String, List<String>> entry : config.trips.entrySet()){
    	  String passengerName = entry.getKey();
    	  String metroCardId = config.metroCard.get(passengerName);
    	  double balance = MetroCardDatabase.getMetroCardData(metroCardId);
    	  double newBalance = balance - 2.9;
    	  MetroCardDatabase.updateMetroCardBalance(metroCardId, newBalance);
    	  System.out.println(passengerName + "'s Metro Card("+metroCardId+") original balance : "+balance);
    	  System.out.println(passengerName + "'s Metro Card("+metroCardId+") New balance : "+newBalance);
    	  List<String> tripStops = entry.getValue();
          addJourney(passengerName,tripStops);
      }
      passengerMetroCard = config.metroCard;

    }catch(JsonParseException | IOException e){
      e.printStackTrace();
    }
  }


  public boolean passengerOnBoard(Passenger passenger){
    for(Set<Passenger> p: trainPassenger.values()){
      if(p.contains(passenger)){
        return true;
      }
    }
    return false;
  }

  public boolean hasStopForward(Train t, Station s){
    if(!trainsStops.get(t).contains(s)){
      return false;
    }
    boolean forward = trainsDirection.get(t);
    Station curStation = trainsPos.get(t);
    List<Station> stations = trainsStops.get(t);
    if(forward){
      return stations.indexOf(curStation) <= stations.indexOf(s);
    }
    return stations.indexOf(curStation) > stations.indexOf(s);
  }

  private JTextField createStationField(String lineName, String stationName, List<Integer> point) {
    int x = point.get(0);
    int y = point.get(1);
    JTextField stationField = new JTextField(stationName);
    Font originalFont = stationField.getFont();
    Font smallerFont = originalFont.deriveFont(originalFont.getSize() - 4f);
    stationField.setFont(smallerFont);
    stationField.setBounds(x,y,60,20);
    stationField.setEditable(false);
    stationField.setHorizontalAlignment(JTextField.CENTER);
    switch(lineName){
      case "F":
        stationField.setBackground(Color.ORANGE);
        break;
      case "A":
        stationField.setBackground(Color.CYAN);
        break;
      case "N":
        stationField.setBackground(Color.YELLOW);
        break;
      default:
        stationField.setBackground(Color.GRAY);

    }



    return stationField;
  }
  private static class LineComponent extends JComponent {
    private final JTextField stationA;
    private final JTextField stationB;

    public LineComponent(JTextField stationA, JTextField stationB) {
      this.stationA = stationA;
      this.stationB = stationB;

      setBounds(0,0,800,700);


    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(Color.BLACK); 
      g.drawLine(stationA.getX() + stationA.getWidth() / 2, stationA.getY() + stationA.getHeight() / 2,
              stationB.getX() + stationB.getWidth() / 2, stationB.getY() + stationB.getHeight() / 2);

    }
  }

}
