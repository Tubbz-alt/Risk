package JCK.Risk.Gameplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import JCK.Risk.Locations.Continent;
import JCK.Risk.Locations.Territory;
import JCK.Risk.Players.Player;

public class Turns {
	
	Card cards = new Card();
	
	public Turns(Game game) throws IOException {
		int playerTurnCount = 0;
		System.out.println("initialized the turns");
		
		while (game.getPlayersArray().size() != 1) { //
			//Game pastGame = new Game(game);
			Undo undo = new Undo(game.playersArray, game.continentArray);

			Player player = game.getPlayersArray().get(playerTurnCount);
			
			System.out.println("It is " + player.getName() + "'s turn.");
			
			System.out.println("CARD PHASE");
			System.out.println("The next set turned in will grant " + cards.getNextSetValue() + "additional units");
			int additionalUnits = cards.checkCards(player);
			
			
			System.out.println("\nPLACE  NEW ARMIES PHASE");
			additionalUnits+= getExtraArmiesForContinentsOwned(player, game.getContinentArray());
			additionalUnits+= getExtraArmiesForTerrsOwned(player);
			
			placeNewSoldiers(game, player, additionalUnits);
			

			System.out.println("\nBATTLE PHASE");
			boolean playerWonABattle = attackingPhaseFor(player, game.getContinentArray(), game);

			playerTurnCount = playerTurnCount + numLeftSidePlayersEliminated(game, player);
			
			if(playerWonABattle == true)
			{
				String cardWon = cards.getCard();
				System.out.println("For winning at least 1 battle, you win card of type " + cardWon);
				player.addCardToList(cardWon);
				
			}
			
			
			
			System.out.println("\nFORTIFY PHASE");
			fortifyTerritory(player, game.getContinentArray());
			
			// if the player chooses to undo their turn -- returns to the beginning of the loop without
			// finishing loop
			if (undoTurn(game, undo)) {
				continue;
			}
			playerTurnCount++;
			playerTurnCount %= game.getPlayersArray().size();


			additionalUnits = 0;
			System.out.println("END OF TURN\n\n");
		}
	}
	
	public boolean undoTurn(Game game, Undo undo) {
		System.out.println("\nWould you like to undo the last turn? Yes or no?");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			String userInput = br.readLine();
			userInput = userInput.toLowerCase();
			if (userInput.equals("yes")) {
				if (undo != null) {
					System.out.println("Undoing to the beginning of your turn.");
					
					//sets the continent array and players array to the previous state
					game.continentArray = (ArrayList<Continent>) undo.getPastContinent();
					game.playersArray = (ArrayList<Player>) undo.getPastPlayers();
					
					return true;
				} else {
					System.out.println("Cannot undo any turns because no turns have occurred.");
					return false;
				}
			}
		} catch (IOException e) {
			System.out.println("Invalid input");
		}
		
		return false;

	}
	
	public void fortifyTerritory(Player player, ArrayList<Continent> continentArray) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("You may choose to move units from 1 owned territory to another, type yes if you want to");
		String userInput = br.readLine().toLowerCase();
		
		if(Objects.equals(userInput, "yes") == false)
		{
			System.out.println("Okay, we have reached the end of the fortify phase");
			return;
		}
		
		displayPlayerTerrs(player, continentArray, "fortify");
		
		
		System.out.println("\nEnter the territory you want to move units from"
				+ " (has to have more than 1 unit, else we skip this phase)");
		userInput = br.readLine();
		while (!playerOwnsTerritory(userInput, player, continentArray)) {
			System.out.println("Please enter a territory you own: ");
			userInput = br.readLine();
		}
		
		Territory donatingTerr = getTerritoryObject(userInput, continentArray);
		
		if(donatingTerr.getSoldierCount() < 2)
		{
			System.out.print("There's only 1 unit here, we are skipping this phase");
			return;
		}
		
		
		System.out.println("\nChoose the territory you want to move units to");
		String newInput = br.readLine();
		while (!playerOwnsTerritory(newInput, player, continentArray) || ( Objects.equals(newInput, userInput) == true )) {
			System.out.println("Please enter a territory you own: ");
			newInput = br.readLine();
		}
		
		Territory receivingTerr = getTerritoryObject(newInput, continentArray);
		
		migrateUnitsFromOldToNewTerritory(donatingTerr, receivingTerr);
		
		System.out.println("We have reached the end of the fortify phase");

	}
	
	
	//if a player to the left side of array is eliminated, it affects out player
	//turn count formula
	public int numLeftSidePlayersEliminated(Game game, Player currentPlayersTurn)
	{
		ArrayList<Player> playersArray = game.getPlayersArray();
		int leftSideEliminated = 0;
		
		for(int i = 0; i < playersArray.size(); i++)
		{
			Player thisPlayer = playersArray.get(i);
			
			if(thisPlayer.getTerritoriesOwned().size() == 0)
			{
				if(thisPlayer.rollValue < currentPlayersTurn.rollValue) //if hes to the left of winning player
				{
					leftSideEliminated++;
				}
				System.out.println(thisPlayer.getName() + " has no more territories and has thus been eliminated");
				game.getPlayersArray().remove(i);
				
				//TODO: TRANSFER OVER THE CARDS TO THE WINNER PLAYER WHO ELIMINATED THEM
			}
		}
		
		
		
		return leftSideEliminated;
	}
	
	public int getExtraArmiesForContinentsOwned(Player player, ArrayList<Continent> continentArray)
	{
		int additionalUnits = 0;
		for(int i = 0; i < continentArray.size(); i++)
		{
			if (continentArray.get(i).playerOwnsContinent(player)) //if he owns it
			{
				additionalUnits = additionalUnits + continentArray.get(i).getContinentValue();
			}
		}
		return additionalUnits;
	}
	
	
	public int getExtraArmiesForTerrsOwned(Player player)
	{
		
		if(player.getTerritoriesOwned().size() <= 11)
		{
			return 3;
		}
		
		int additionalUnits = player.getTerritoriesOwned().size() / 3;
		
		return additionalUnits;
	}
	
	
	/**
	 * Places the soldiers that the player has onto territories that the player owns
	 * @param game
	 * @param player
	 */
	public void placeNewSoldiers(Game game, Player player, int numUnitsAvailable) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<Continent> continentArray = game.getContinentArray();
		
		while (numUnitsAvailable > 0) {
			try {
				// asks the user which territory to place soldier onto
				game.displayWorld();
				System.out.println(player.getName() + ", you have " + numUnitsAvailable + " soldiers to place. Choose a territory you own to place: ");
				String userInput = br.readLine();
				while (!playerOwnsTerritory(userInput, player, continentArray)) {
					System.out.println("Please enter a territory you own: ");
					userInput = br.readLine();
				}
				
				// asks the user how many soldier to place on the territory defined in the variable userInput
				System.out.println("How many soldier do you want to place on " + userInput + "?");
				int soldiersToPlace = Integer.parseInt(br.readLine());
				while (soldiersToPlace > numUnitsAvailable || soldiersToPlace < 0) {
					System.out.println("Enter an amount less than or equal to the amount you have to place.");
					soldiersToPlace = Integer.parseInt(br.readLine());
				}
				
				// places the amount of soldiers onto the territory the user defines
				for (int i = 0; i < continentArray.size(); i++) {
					if (continentArray.get(i).getTerritory(userInput) != null) {
						HashMap<String, Territory> listOfTerritories = continentArray.get(i).getListOfTerritories();
						listOfTerritories.put(userInput, listOfTerritories.get(userInput).addSoldiers(soldiersToPlace));
					}
				}
				// subtracts the number of soldiers the player can place by the number of soldiers just placed
				numUnitsAvailable = numUnitsAvailable - soldiersToPlace;
			} catch (IOException e) {
				System.out.println("You have entered an invalid input. Please try again.");
			}
		}
	}
	
	
	/**
	 * Checks if the player owns all the territory
	 * @param userInput
	 * @param player
	 * @param game
	 * @return
	 */
	public boolean playerOwnsTerritory(String userInput, Player player, ArrayList<Continent> continentArray) {
		for (int i = 0; i < continentArray.size(); i++) {
			if (continentArray.get(i).getTerritory(userInput) != null) {
				if (player.getName().equals(continentArray.get(i).getTerritory(userInput).getOwner())) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	public boolean attackingPhaseFor(Player player, ArrayList<Continent> continentArray, Game game) throws IOException
	{
		boolean wonAtLeast1Battle = false;
		displayPlayerTerrs(player, continentArray, "attack");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Do you want to make an attack, yes or no?");
		String option = br.readLine().toLowerCase();
		
		while(Objects.equals("yes", option))
		{
			System.out.println("Okay, type in the name of the territory you want to attack from");
			String attackingTerr = br.readLine();

			
			while (!playerOwnsTerritory(attackingTerr, player, continentArray)) {
				System.out.println("Please enter a territory you own: ");
				attackingTerr = br.readLine();
				
			}
			
			Territory attackingTerritory = getTerritoryObject(attackingTerr, continentArray);
			
			if(attackingTerritory.getSoldierCount() < 2)
			{
				System.out.println("You don't have enough soldiers to attack from here, sorry!");
				System.out.println("Do you want to make another attack, yes or no?");
				option = br.readLine().toLowerCase();
				continue;
			}
			
			
			System.out.println("The territories you can attack from here are");
			ArrayList<Territory> attackableAdjs = checkAttackableAdjacencies(player, attackingTerritory, continentArray);
			if( attackableAdjs.size() == 0)
			{
				System.out.println("NONE, YOU OWN THEM ALL!");
				System.out.println("Do you want to make another attack, yes or no?");
				option = br.readLine().toLowerCase();
				continue;
			}
			
			
			System.out.println("\nType the name of the territory you want to attack. If you don't want to attack one, type anything else.");
			String defendingTerr = br.readLine();
			Territory defendingTerritory = getTerritoryObject(defendingTerr, continentArray);
			
			if(Objects.equals(defendingTerritory, null))
			{
				System.out.println("Okay, you chose not to attack");
				System.out.println("Do you want to make another attack, yes or no?");
				option = br.readLine().toLowerCase();
				continue;
			}
			
			String defTerrOwner = defendingTerritory.getOwner();
			
			System.out.println(defTerrOwner + ", your territory " + defendingTerr + " is being attacked!");
			
			//reaching here means we have a valid defending & attacking terr
			String winnerOfBattle = beginBattle(defendingTerritory, attackingTerritory);
			
			if(Objects.equals(player.getName(), winnerOfBattle)) //if attacker won
			{
				wonAtLeast1Battle = true;
				String losingDefender = defendingTerritory.getOwner();
			
				defendingTerritory.setOwner(player.getName());	
				player.addTerritoryOwned(defendingTerr);
				
				removeTerritoryFromDefender(losingDefender, defendingTerr, game);
				migrateUnitsFromOldToNewTerritory(attackingTerritory, defendingTerritory);
			}
			
			
			System.out.println("\nDo you want to make another attack, yes or no?");
			option = br.readLine().toLowerCase();
		}

		System.out.println("Okay, the attack phase is over");
		return wonAtLeast1Battle;
		
	}
	
	
	
	public Territory getTerritoryObject(String territoryName, ArrayList<Continent> continentArray)
	{
		for(int i = 0; i < continentArray.size(); i++)
		{
			Territory currTerritory = continentArray.get(i).getTerritory(territoryName);
			
			if(currTerritory != null)
			{
				return currTerritory;
			}
		}
		
		return null;
	}
	
	
		
	public ArrayList<Territory> checkAttackableAdjacencies(Player player, Territory attackingTerr, ArrayList<Continent> continentArray)
	{
		List<String> adjacensies = attackingTerr.getAdjacencies();
		ArrayList<Territory> attackables = new ArrayList<Territory>();
		
		for(int i = 0; i < adjacensies.size(); i++)
		{
			Territory adjTerr = getTerritoryObject(adjacensies.get(i), continentArray);
			
			if(Objects.equals(adjTerr.getOwner(), player.getName()))  //if attacker owns it
			{
				continue;
			}
			
			System.out.println(adjTerr.getTerritoryName() + " which has " + adjTerr.getSoldierCount() + " soldier(s) on it");
			attackables.add(adjTerr);
			
		}
		return attackables;
	}
	
	
	public void displayPlayerTerrs(Player player, ArrayList<Continent> continentArray, String phaseType)
	{
		System.out.println("THESE ARE THE TERRITORIES YOU OWN");
		for(int i = 0; i < player.getTerritoriesOwned().size(); i++)
		{
			String terrName = player.getTerritoriesOwned().get(i);
			System.out.print(i + ". " + terrName);
			
			for(int j = 0; j < continentArray.size(); j++)
			{
				Territory currTerritory = continentArray.get(j).getTerritory(terrName);
				
				if(currTerritory != null)
				{
					System.out.println(", the number of soldiers you have here are " + currTerritory.getSoldierCount());
					if(Objects.equals(phaseType, "attack"))
					{
						System.out.println("These are the territories you can attack from here:");
						System.out.println(currTerritory.getAdjacencies());
						System.out.println();
						
					}
					
					
					break;
				}
			}
		}
		
	}
	
	
	
	public void migrateUnitsFromOldToNewTerritory(Territory donaterTerr, Territory receiverTerr)
	{
		int currNumSoldiers = donaterTerr.numSoldiersHere;

		if(currNumSoldiers == 2)
		{
			System.out.println("You moved 1 soldier to your receiving territory");
		}
		
		
		System.out.println("You must reinforce your receiving territory with at least 1 soldier.");
		System.out.println("You currently have " + currNumSoldiers + " in your donating territory.");
		System.out.println("Input a valid number, else we will only be moving one to your receiving territory");
		
		int migratingSoldiers = 1;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			migratingSoldiers = Integer.parseInt(br.readLine());
		} catch (IOException e) {
			System.out.println("Okay, 1 it is");
			migratingSoldiers = 1;
		}
		
		
		int numSoldiersStayed = currNumSoldiers - migratingSoldiers;
		
		donaterTerr.numSoldiersHere = numSoldiersStayed;
		receiverTerr.numSoldiersHere = migratingSoldiers;

	}
	

	
	public void removeTerritoryFromDefender(String defenderName, String territoryLost, Game game)
	{
		ArrayList<Player> playersArray = game.getPlayersArray();
		
		for(int i = 0; i < playersArray.size(); i++)
		{
			Player currPlayer = playersArray.get(i);
			if(Objects.equals(currPlayer.getName(), defenderName))  //if we have the player object
			{
				currPlayer.removeTerritoryOwned(territoryLost);
				break;
				
			}
		}
	}
	
	
	
	//TODO: RENAME TO LET USER KNOW DEFENDER IS ON THE LEFT?
	public String beginBattle(Territory defendingTerr, Territory attackingTerr)
	{
		String attackerName = attackingTerr.getOwner();
		String defenderName = defendingTerr.getOwner();
		
		//int maxAttackingSoldiers = attackingTerr.numSoldiersHere - 1;
		//int maxDefendingSoldiers = defendingTerr.numSoldiersHere - 1;
		
		while(attackingTerr.numSoldiersHere != 1 || defendingTerr.numSoldiersHere != 0)
		{
			int attackingSoldiers = this.numAttackers(attackingTerr);
			int defendingSoldiers = this.numDefenders(defendingTerr);
			
			System.out.println(attackerName + " your rolls are "); 
			Integer[] attackerRolls = this.getRollsSorted(attackingSoldiers);
			
			System.out.println(defenderName + " your rolls are "); 
			Integer[] defenderRolls = this.getRollsSorted(defendingSoldiers);
						
			int i = 0;
			
			System.out.println("The results are:");
			while(i < defenderRolls.length && i < attackerRolls.length)
			{
				System.out.print("The winner for the roll " + (i+1)  + " is ");
				
				if(attackerRolls[i] > defenderRolls[i])
				{
					System.out.println(attackerName);
					defendingTerr.numSoldiersHere -= 1;
				}
				else
				{
					System.out.println(defenderName);
					attackingTerr.numSoldiersHere -= 1;

				}
				
				i++;
				
			}
			
			System.out.println(attackerName + " remaining soldiers are " + attackingTerr.numSoldiersHere);
			System.out.println(defenderName + " remaining soldiers are " + defendingTerr.numSoldiersHere);
			
			if(attackingTerr.numSoldiersHere == 1 || defendingTerr.numSoldiersHere == 0)
			{
				break;
			}
		}
		
		
		System.out.print("The winner of this battle is: ");
		if(attackingTerr.numSoldiersHere == 1)
		{
			System.out.println(defenderName);
			return defenderName;
		}
		else
		{
			System.out.println(attackerName);
			System.out.println(defendingTerr.territoryName + " is now yours");
			return attackerName;
		}
		

	}
	
	
	public Integer[] getRollsSorted(int numberOfRolls)
	{
		Integer[] rollsArray = new Integer[numberOfRolls];
		
		for(int i = 0; i < rollsArray.length; i++)
		{
			rollsArray[i] = Dice.roll();
			System.out.println(rollsArray[i]);
			
		}
		
		
		Arrays.sort(rollsArray, Collections.reverseOrder());
		
		return rollsArray;
	}
	
	public int numAttackers(Territory attackingTerr)
	{
		if(attackingTerr.numSoldiersHere > 3)
		{
			return 3;
		}
		else if(attackingTerr.numSoldiersHere == 3)
		{
			return 2;
		}
		else  //if == 2
		{
			return 1;
		}
		
	}
	
	public int numDefenders(Territory defendingTerr)
	{
		if(defendingTerr.numSoldiersHere >= 2)
		{
			return 2;
		}
		else
		{
			return 1;
		}
		
	}
	
	public void attackTerritory(String territoryName, Continent currContinent)
	{
		Territory terrBeingAttacked = currContinent.getTerritory(territoryName);
		
		
	}
}
