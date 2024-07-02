public class Verify {

  public static void verify(MTB MTB, Log log) {
    MTB.checkStart();
    for (Event e : log.events()) {
      e.replayAndCheck(MTB);
    }
    MTB.checkEnd();
  }
}
