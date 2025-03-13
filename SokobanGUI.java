package exercise.codesmell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SokobanGUI extends JFrame {
	private static final int TILE_SIZE = 50;
	private static final int GRID_ROWS = 8; // Include border rows
	private static final int GRID_COLS = 8; // Include border columns

	private static final char PLAYER = '@';
	private static final char BOX = 'B';
	private static final char GOAL = 'G';
	private static final char WALL = '#';
	private static final char EMPTY = ' ';

	private String[][] levels = { { "######", "#    #", "#   G#", "# G  #", "######" },
			{ "      ", " @    ", "   B  ", "   B  ", "      " },
			{ "####### ", "#     ##", "#      #", "# #G  G#", "#      #", "########" },
			{ "        ", " @      ", "  BB    ", "        ", "        ", "        " },
			{ "  #### ", "###  ##", "#  G  #", "#     #", "# #G  #", "#     #", "#######" },
			{ "       ", "       ", " @  B  ", "    B  ", "       ", "       ", "       " },
			{ " ##### ", "##   ##", "#  #  #", "#  G  #", "#  G  #", "#  G  #", "#######" },
			{ "       ", "  @    ", "       ", "  BBB  ", "       ", "       ", "       " },
			{ "######  ", "#    ###", "#   GG #", "#      #", "#  # G #", "########" },
			{ "        ", "        ", "        ", "  BBB@  ", "        ", "        " } };

	private int currentLevel = 0;
	private char[][] level, stats;
	private int playerRow;
	private int playerCol;

	private JPanel gamePanel;

	private static Map<String, String> registeredUsers = new HashMap<>();
	private String currentUser = null;

	public SokobanGUI() {
		if (!login()) {
			JOptionPane.showMessageDialog(this, "Exiting game. Login required.");
			System.exit(0);
		}

		loadSavedProgress();
		setTitle("Sokoban Game");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(GRID_COLS * TILE_SIZE, (GRID_ROWS + 1) * TILE_SIZE); // Extra row for level indicator
		setResizable(false);

		loadLevel(currentLevel);

		gamePanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawLevel(g);
			}
		};
		gamePanel.setPreferredSize(new Dimension(GRID_COLS * TILE_SIZE, (GRID_ROWS + 1) * TILE_SIZE));
		add(gamePanel);

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				processMove(e.getKeyCode());
				gamePanel.repaint();
			}
		});

		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		addSaveLoadMenu();
	}

	private boolean login() {
		while (true) {
			int choice = getUserChoice();
			if (choice == 0)
				return true;
			if (choice == 1 && loginUser())
				return true;
			if (choice == 2)
				registerUser();
			else
				return false;
		}
	}

	private int getUserChoice() {
		String[] options = { "Guest", "Login", "Register" };
		return JOptionPane.showOptionDialog(null,
				"Welcome to Sokoban! Please choose an option:",
				"Login",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				options,
				options[0]);
	}

	private boolean loginUser() {
		String username = JOptionPane.showInputDialog("Enter username:");
		if (username == null)
			return false;

		String password = JOptionPane.showInputDialog("Enter password:");
		if (password == null)
			return false;

		if (isValidCredentials(username, password)) {
			JOptionPane.showMessageDialog(null, "Login successful! Welcome, " + username + "!");
			currentUser = username;
			return true;
		} else {
			JOptionPane.showMessageDialog(null, "Invalid username or password. Try again.");
			return false;
		}
	}

	private boolean isValidCredentials(String username, String password) {
		return registeredUsers.containsKey(username) && registeredUsers.get(username).equals(password);
	}

	private void registerUser() {
		String username = JOptionPane.showInputDialog("Choose a username:");
		if (username == null)
			return;

		String password = JOptionPane.showInputDialog("Choose a password:");
		if (password == null)
			return;

		if (isUsernameTaken(username)) {
			JOptionPane.showMessageDialog(null, "Username already exists. Please choose another.");
		} else {
			registeredUsers.put(username, password);
			JOptionPane.showMessageDialog(null, "Registration successful! Please log in.");
		}
	}

	private boolean isUsernameTaken(String username) {
		return registeredUsers.containsKey(username);
	}

	private void loadLevel(int levelIndex) {
		String[] levelStrings = levels[levelIndex * 2];
		String[] statsStrings = levels[levelIndex * 2 + 1];
		level = new char[levelStrings.length][levelStrings[0].length()];
		stats = new char[statsStrings.length][statsStrings[0].length()];

		for (int row = 0; row < levelStrings.length; row++) {
			level[row] = levelStrings[row].toCharArray();
			stats[row] = statsStrings[row].toCharArray();
		}

		findPlayerPosition();
	}

	private void findPlayerPosition() {
		for (int row = 0; row < level.length; row++) {
			for (int col = 0; col < level[row].length; col++) {
				if (stats[row][col] == PLAYER) {
					playerRow = row;
					playerCol = col;
					return;
				}
			}
		}
	}

	private void drawLevel(Graphics g) {
		drawLevelIndicator(g);
		drawGameGrid(g);
		drawCurrentPlayer(g);
	}

	private void drawLevelIndicator(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, GRID_COLS * TILE_SIZE, TILE_SIZE);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.drawString("Level: " + (currentLevel + 1), 10, 30);
	}

	private void drawGameGrid(Graphics g) {
		for (int row = 0; row < level.length; row++) {
			for (int col = 0; col < level[row].length; col++) {
				drawCell(g, row, col);
			}
		}
	}

	private void drawCell(Graphics g, int row, int col) {
		char cell = level[row][col];
		char stat = stats[row][col];
		int x = col * TILE_SIZE;
		int y = (row + 1) * TILE_SIZE;

		if (stat == BOX) {
			drawBox(g, x, y);
		} else if (stat == PLAYER) {
			drawPlayerAt(g, x, y);
		}

		switch (cell) {
			case WALL:
				drawWall(g, x, y);
				break;
			case GOAL:
				drawGoal(g, x, y);
				break;
		}
	}

	private void drawBox(Graphics g, int x, int y) {
		g.setColor(Color.ORANGE);
		g.fillRoundRect(x + 10, y + 10, TILE_SIZE - 20, TILE_SIZE - 20, 15, 15);
	}

	private void drawCurrentPlayer(Graphics g) {
		int x = playerCol * TILE_SIZE;
		int y = (playerRow + 1) * TILE_SIZE;
		drawPlayerAt(g, x, y);
	}

	private void drawPlayerAt(Graphics g, int x, int y) {
		g.setColor(new Color(173, 216, 230)); // Light blue
		g.fillOval(x + 15, y + 15, TILE_SIZE - 30, TILE_SIZE - 30);
	}

	private void drawWall(Graphics g, int x, int y) {
		g.setColor(Color.LIGHT_GRAY);
		g.fillRoundRect(x, y, TILE_SIZE, TILE_SIZE, 15, 15);
		g.setColor(Color.DARK_GRAY);
		g.drawRoundRect(x, y, TILE_SIZE, TILE_SIZE, 15, 15);
	}

	private void drawGoal(Graphics g, int x, int y) {
		g.setColor(Color.GREEN);
		int[] xPoints = { x + TILE_SIZE / 2, x + TILE_SIZE - 15, x + TILE_SIZE / 2, x + 15 };
		int[] yPoints = { y + 15, y + TILE_SIZE / 2, y + TILE_SIZE - 15, y + TILE_SIZE / 2 };
		g.fillPolygon(xPoints, yPoints, 4);
	}

	private boolean isGameWon() {
		for (int row = 0; row < level.length; row++)
			for (int col = 0; col < level[row].length; col++)
				if (level[row][col] == GOAL && stats[row][col] != BOX)
					return false;
		return true;
	}

	private void processMove(int keyCode) {
		int newRow = playerRow;
		int newCol = playerCol;

		switch (keyCode) {
			case KeyEvent.VK_UP:
				newRow--;
				break;
			case KeyEvent.VK_LEFT:
				newCol--;
				break;
			case KeyEvent.VK_DOWN:
				newRow++;
				break;
			case KeyEvent.VK_RIGHT:
				newCol++;
				break;
			default:
				return;
		}

		if (level[newRow][newCol] == WALL)
			return;

		if (stats[newRow][newCol] == BOX) {
			int boxNewRow = newRow + (newRow - playerRow);
			int boxNewCol = newCol + (newCol - playerCol);
			if (stats[boxNewRow][boxNewCol] == BOX)
				return;
			if (level[boxNewRow][boxNewCol] == EMPTY || level[boxNewRow][boxNewCol] == GOAL) {
				stats[boxNewRow][boxNewCol] = BOX;
				stats[newRow][newCol] = PLAYER;
				stats[playerRow][playerCol] = EMPTY;
				playerRow = newRow;
				playerCol = newCol;
			}
		} else if (level[newRow][newCol] == EMPTY || level[newRow][newCol] == GOAL) {
			stats[newRow][newCol] = PLAYER;
			stats[playerRow][playerCol] = EMPTY;
			playerRow = newRow;
			playerCol = newCol;
		}

		if (isGameWon()) {
			currentLevel++;
			if (currentLevel * 2 < levels.length) {
				loadLevel(currentLevel);
				gamePanel.repaint();
			} else {
				JOptionPane.showMessageDialog(this, "Congratulations! You completed all levels!");
				System.exit(0);
			}
		}
	}

	private void addSaveLoadMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Game");
		JMenuItem saveItem = new JMenuItem("Save");
		JMenuItem loadItem = new JMenuItem("Load");

		saveItem.addActionListener(e -> saveProgress());
		loadItem.addActionListener(e -> loadSavedProgress());

		menu.add(saveItem);
		menu.add(loadItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
	}

	private void saveProgress() {
		if (currentUser == null) {
			JOptionPane.showMessageDialog(this, "Save feature is available only for registered users.");
			return;
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentUser + "_progress.dat"))) {
			oos.writeInt(currentLevel);
			oos.writeObject(level);
			JOptionPane.showMessageDialog(this, "Game progress saved successfully!");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error saving progress: " + e.getMessage());
		}
	}

	private void loadSavedProgress() {
		if (currentUser == null)
			return;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(currentUser + "_progress.dat"))) {
			currentLevel = ois.readInt();
			loadLevel(currentLevel);
			// level = (char[][]) ois.readObject();
			findPlayerPosition();
			gamePanel.repaint();
			JOptionPane.showMessageDialog(this, "Game progress loaded successfully!");
		} catch (IOException e) {
			// No saved progress found
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(SokobanGUI::new);
	}
}
