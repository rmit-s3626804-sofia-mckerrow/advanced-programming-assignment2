package gameDatabase;
// @author Calvin

import java.io.BufferedWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gameComponents.Athlete;
import gameComponents.Cycle;
import gameComponents.Cyclist;
import gameComponents.Game;
import gameComponents.Official;
import gameComponents.Participant;
import gameComponents.Run;
import gameComponents.Runner;
import gameComponents.SuperAthlete;
import gameComponents.Swim;
import gameComponents.Swimmer;
import gui.MenuModel;

public class DataBase {
	
	private static Connection con;
	private static Connection connection = SQLiteConnection.connector();
	private MenuModel menuModel = new MenuModel();
	// private static boolean hasPartData = false;
	// private static boolean hasResData = false;
	private ArrayList<Athlete> athletes = new ArrayList<Athlete>();			// temporary array list to read in athletes
	private ArrayList<Official> officials = new ArrayList<Official>(); 		// temporary array list to read in officials
	private ArrayList<Game> games = new ArrayList<Game>();					// array list of games
	
	public ArrayList<Athlete> getAthletes() {
		return athletes;
	}
	
	public ArrayList<Official> getOfficials() {
		return officials;
	}
	
	public ArrayList<Game> getGames() {
		return games;
	}

	public void setAthletes(ArrayList<Athlete> athletes) {
		this.athletes = athletes;
	}

	public void setGames(ArrayList<Game> games) {
		this.games = games;
	}
	
	// check if entries are valid
	public boolean validEntryCheck(String regularExpression, String stringToCheck) {
		Pattern checkRegEx = Pattern.compile(regularExpression);
		Matcher regexMatcher = checkRegEx.matcher(stringToCheck);
		while (regexMatcher.find()){
			if (regexMatcher.group().length() != 0){
				return true;
			}			
		}
		return false;
	}
	
	public void readParticipantsFromFile() throws FileNotFoundException {
		Scanner fileIn = new Scanner(new File("Assets/Participants.txt"));
		boolean fieldIsValid = false;
		while(fileIn.hasNextLine()) {
			String[] props = fileIn.nextLine().split(", ");
			// possible regular expressions to check each field is valid
			String idCheck = "[a-zA-Z0-9]{4}"; 		// a to z lower/upper case and 0 to 9 and length 4
			String nameCheck = "^[a-zA-z ]*$";  	// a to z lower/upper and spaces
			String typeCheck = "^[a-zA-Z]*$";		// a to z lower/upper
			String ageCheck = "\\d{2}";				// digits length 2
			String stateCheck = "[A-Za-z]{2,3}";	// a to z lower upper length 2 to 3
			// if at least one of the fields are invalid skip the entry
			try {
				if (validEntryCheck(idCheck, props[0]) == true && validEntryCheck(nameCheck, props[1]) == true && validEntryCheck(typeCheck, props [2]) == true && validEntryCheck(ageCheck, props[3]) == true && validEntryCheck(stateCheck, props[4]) == true) {
					fieldIsValid = true;
				}
			}
			catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(e);
			}
			catch (NumberFormatException e) {
				System.out.println(e);
			}
			// check fields and length to ensure no incomplete participant is added
			if (props.length == 5 && fieldIsValid == true) {
				sortParticipantsIntoType(props[0], props[1], props[2], Integer.valueOf(props[3]), props[4]);
			}
		}
		fileIn.close();
	}
	
	public void sortParticipantsIntoType(String id, String name, String type, int age, String state) {
		if (type.equals("Official")) {
			Official thisOfficial = new Official(id, name, type, age, state);
			officials.add(thisOfficial);
		}
		else {
			Athlete thisAthlete = null;
			if (type.equals("Swimmer")) thisAthlete = new Swimmer(id, name, type, age, state);
			if (type.equals("Runner")) thisAthlete = new Runner(id, name, type, age, state);
			if (type.equals("Cyclist")) thisAthlete = new Cyclist(id, name, type, age, state);
			if (type.equals("SuperAthlete")) thisAthlete = new SuperAthlete(id, name, type, age, state);
			athletes.add(thisAthlete);
		}
	}
	
	public void recordGame() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException{
		// true = append / false = overwrite
		boolean writeNew = false;
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(
				"Assets/gameResults.txt", writeNew));
			//write game, officialID, date
			// eg	s02, o012, Sun Apr 16 18:35:07 AEST 2017
			//write athlete, time, points for race
			// eg	a038, 10.6, 5
			for (int i = 0; i < games.size(); i++) {
				Game recordGame = games.get(i);
				String logID = recordGame.getRaceID();
				String logOfficialID = recordGame.getRaceOfficial().getID();
				String logDate = recordGame.getDate();
				writer.write(logID + ", " + logOfficialID + ", " + logDate);
				writer.newLine();
				for (int j = 0; j < recordGame.getRaceAthletes().size(); j++){
					String id = recordGame.getRaceAthletes().get(j).getID();
					double time = recordGame.getResultArray().get(j);
					int points = recordGame.getRaceAthletes().get(j).getRoundPoints();
					writer.write(id + ", " + time + ", " + points); 
					writer.newLine();
					this.addResultToDatabase(id, time, points, logID, logOfficialID, logDate);
				}
				writer.newLine();
			}
		writer.close();
	}
	
	// record game to database and results.txt file
		public void recordLastGame() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
			Game myGame = getLastGame();			
			String gameID = myGame.getRaceID();
			String officialID = myGame.getRaceOfficial().getID();
			String date = myGame.getDate();
			
			for (int i = 0; i < myGame.getRaceAthletes().size(); i++){ 		
				String athleteID = myGame.getRaceAthletes().get(i).getID();
				double time = myGame.getRaceAthletes().get(i).getRoundTime();
				int points = myGame.getRaceAthletes().get(i).getRoundPoints();
				
				if (menuModel.isDbConnected()) {
					addResultToDatabase(athleteID, time, points, gameID, officialID, date);
				}
			}
		}
	
	// to delete
	public ArrayList<Athlete> getShuffledAthletes(){ 
		ArrayList<Athlete> shuffled = athletes;	
		Collections.shuffle(shuffled);
		return shuffled;
	}
	
	public Game addRace(String raceType) { 		
		// generate raceID and pass it to a new Game in the arraylist of Games
		String raceID = Game.getNextID(raceType, this.games.size());
		Game thisGame = null;
		if (raceType.equals("swim")) thisGame = new Swim(raceID);
		if (raceType.equals("run")) thisGame = new Run(raceID);
		if (raceType.equals("cycle")) thisGame = new Cycle(raceID);
		this.games.add(thisGame);
		return thisGame;
	}
	
	public void addGame(Game thisGame) {
		this.games.add(thisGame);
	}
	
	public Game getLastGame() {					
		// retrieve the last game in the list of games
		int lastIndex = (games.size() - 1);
		return games.get(lastIndex);
		// can throw index out of bounds - checks elsewhere avoid this
	}
	
	public void getResultList() {
		// check for races and print formatted list of race results
		if (games.isEmpty() == true){
			System.out.println("No races to display!");
		}
		else {
			for (int i = 0; i < games.size(); i++){
				Game getGame = games.get(i);
				int resultSize = getGame.getResultArray().size();
				System.out.println();
				System.out.println("Race " + getGame.getRaceID() + " Results");
				System.out.println("   Name			Time (seconds)");
				for (int j = 0; j < resultSize; j++){	// print results
					Athlete getAthlete = getGame.getRaceAthletes().get(j);
					double myTime = getGame.getResultArray().get(j);
					System.out.print((j+1) +". ");
					System.out.printf("%-20s %8s %n", getAthlete.getName(), myTime);
				}
			}
		}
	}
	
	public void sortAthletesByPointsThenPrint() {  
		// sort athletes by total points
		Collections.sort(athletes, (athlete1, athlete2) -> athlete1.getTotalPoints() - athlete2.getTotalPoints());
		// print
		System.out.printf("%-20s %10s %n", "Name", "Points");
		for (int i = (athletes.size() - 1); i >= 0; i--) {
			Athlete sortPoints = athletes.get(i);
			System.out.printf("%-20s %7s %n", sortPoints.getName(), sortPoints.getTotalPoints());
		}
	}
	
	
//	SQLite
	
	public ResultSet displayParticipants() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		if(con == null) {
			getConnection();
		}
		Statement state = con.createStatement();
		ResultSet res = state.executeQuery("SELECT * FROM participants");
		return res;
	}
	
	public ResultSet displayResults() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		if(con == null) {
			getConnection();
		}
		Statement state = con.createStatement();
		ResultSet res = state.executeQuery("SELECT * FROM results");
		return res;
	}
	
	public void getConnection() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
		Class.forName("org.sqlite.JDBC"); //.newInstance()
		con = DriverManager.getConnection("jdbc:sqlite:ozlympics.db");
		initialise();
		emptyResults();
	}

	public void initialise() throws SQLException {
		// build table
		System.out.println("Building the participants table.");
		Statement state = con.createStatement();
		ResultSet res = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='participants'");
		if(!res.next()){
			// create participants table
			state.execute("CREATE TABLE IF NOT EXISTS participants (id varchar(4),"
					+ "name varchar(40)," + "type varchar(20),"
					+ "age integer," + "state varchar(3));");
			// insert data
			PreparedStatement prep = con.prepareStatement("INSERT INTO participants values(?,?,?,?,?);");
			// iterate through athletes and add to participants table
			for (Athlete add : athletes) {
				prep.setString(1, add.getID());
				prep.setString(2, add.getName());
				prep.setString(3, add.getType());
				prep.setInt(4, add.getAge());
				prep.setString(5, add.getState());
				prep.execute();
			}
			// iterate through officials and add to participants table
			for (Official add : officials) {
				prep.setString(1, add.getID());
				prep.setString(2, add.getName());
				prep.setString(3, add.getType());
				prep.setInt(4, add.getAge());
				prep.setString(5, add.getState());
				prep.execute();
			}
		}
		// build table
		System.out.println("Building the results table.");
		Statement state2 = con.createStatement();
		ResultSet res2 = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='results'");
		if(!res.next()){
//			// create results table
			state.execute("CREATE TABLE IF NOT EXISTS results (athleteID varchar(4),"
					+ "result integer," + "score integer,"
					+ "gameID varchar(8)," + "officialID varchar(4),"
					+ "date varchar(20));");
		}	
	}
	
	// initialise arraylist of Athletes from database
	public ArrayList<Athlete> initialiseAthletesList() {
		String query = "SELECT id, name, type, age, state FROM participants WHERE id LIKE 'a%'";
		
		try {
			PreparedStatement prep = connection.prepareStatement(query);
			ResultSet resultSet = prep.executeQuery();
			
			Athlete thisAthlete = null;
			while (resultSet.next()) {
				// System.out.println(resultSet.getString("id") + "\t" + resultSet.getString("name"));
				// thisAthlete = new Athlete(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				if (resultSet.getString("type").equals("Swimmer")) thisAthlete = new Swimmer(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				if (resultSet.getString("type").equals("Runner")) thisAthlete = new Runner(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				if (resultSet.getString("type").equals("Cyclist")) thisAthlete = new Cyclist(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				if (resultSet.getString("type").equals("SuperAthlete")) thisAthlete = new SuperAthlete(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				athletes.add(thisAthlete);
				//sortParticipantsIntoType(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
			}
			/*for (int i = 0; i < athletes.size(); i++) {
				System.out.println(athletes.get(i).getID() + " " + athletes.get(i).getName());
			}*/
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return athletes;
		
	}
	
	// initialise arraylist of Officials from database
	public ArrayList<Official> initialiseOfficialsList() {
		String query = "SELECT id, name, type, age, state FROM participants WHERE id LIKE 'o%'";
		
		try {
			PreparedStatement prep = connection.prepareStatement(query);
			ResultSet resultSet = prep.executeQuery();
			
			Official thisOfficial = null;
			while (resultSet.next()) {
				thisOfficial = new Official(resultSet.getString("id"), resultSet.getString("name"), resultSet.getString("type"), resultSet.getInt("age"), resultSet.getString("state"));
				officials.add(thisOfficial);
			}
			/*for (int i = 0; i < officials.size(); i++) {
				System.out.println(officials.get(i).getID() + " " + officials.get(i).getName());
			}*/
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return officials;
	}
	
	// deletes all records from results table in database
	public void emptyResults() throws SQLException {
		Statement state = connection.createStatement();
		state.executeUpdate("DELETE FROM results;");
	}
	
	// add game results to database
	public void addResultToDatabase(String athleteID, double time, int points, String gameID, String officialID, String date) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		PreparedStatement prep = connection.prepareStatement("INSERT INTO results values(?,?,?,?,?,?);");
		prep.setString(1, athleteID);
		prep.setDouble(2, time);
		prep.setInt(3, points);
		prep.setString(4, gameID);
		prep.setString(5, officialID);
		prep.setString(6, date);
		prep.execute();
	}
	
	// add game results to file results.txt
	public void addResultToFile() {
		
	}
}