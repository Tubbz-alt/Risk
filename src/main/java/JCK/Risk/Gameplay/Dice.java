package JCK.Risk.Gameplay;
import java.util.Random;

public class Dice {
	private static int diceValue = 0;
	public static int getDiceValue() {
		return diceValue;
	}
	public static int roll() {
		Random random = new Random();
		diceValue = random.nextInt(6) + 1;
		return diceValue;
	}
}
