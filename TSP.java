import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.ArrayList;

/**
 * Simple GA for the Travelling Salesman Problem.
 * 
 * @author Matthew Mansell 
 * @version v0.5
 */
public class TSP
{
    // ########## GA SETTINGS ##########
    private int SIZE; // The number of cities of the TSP instance.
    private int[][] COST; // TSP cost matrix
    private static final int POPULATION_SIZE = 50; // The population size
    private static final int MAX_GENERATION = 999; // The number of generations
    private static final int TOURNAMENT_SIZE = 10; // The tournament size
    
    private Random random = new Random(); // Accessible random generator
    private int[][] population; // Current population
    private int[] fitness = new int[POPULATION_SIZE]; // Individuals fitness
    private int bestResult; //Store for best result
    private int[][] childrenTest;
    
    /**
     * Starts the execution of the GA.
     * 
     * @param filename the TSP file instance.
     */
    public void run(String filename) {
        load(filename); // Load the TSP problem cost matrix
        population = new int[POPULATION_SIZE][SIZE]; // Initialise population now SIZE is set
        System.out.println("Size: "+SIZE);
        initialise(); // Initialise the population
        evaluate(); // Evaluate the initial population
        childrenTest= crossover(select(),select());
        
        // for(int g = 0; g < MAX_GENERATION; g++) {
            // int[][] nextGeneration = generatePopulation(); // Create new population
            // population = nextGeneration; // Copy the new population over
            // System.out.println("Gen "+g+" | "+generationStats()); // Print stats
            // evaluate(); // Evaluate the new population
        // }
        System.out.println("Best Route: "); // Print best result
        
    }
    
    /**
     * Loads the TSP file. This method will initialise the variables
     * size and COST.
     */
    private void load(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = null;
            
            int row = 0;
            int column = 0;
            boolean read = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DIMENSION")) {
                    String[] tokens = line.split(":");
                    SIZE = Integer.parseInt(tokens[1].trim());
                    COST = new int[SIZE][SIZE];
                }
                else if (line.startsWith("EDGE_WEIGHT_TYPE")) {
                    String[] tokens = line.split(":");
                    if (tokens.length < 2 || !tokens[1].trim().equals("EXPLICIT"))
                    {
                        throw new RuntimeException("Invalid EDGE_WEIGHT_TYPE: " + tokens[1]);
                    }
                }
                else if (line.startsWith("EDGE_WEIGHT_FORMAT")) {
                    String[] tokens = line.split(":");
                    if (tokens.length < 2 || !tokens[1].trim().equals("LOWER_DIAG_ROW"))
                    {
                        throw new RuntimeException("Invalid EDGE_WEIGHT_FORMAT: " + tokens[1]);
                    }
                }
                else if (line.startsWith("EDGE_WEIGHT_SECTION")) {
                    read = true;
                }
                else if (line.startsWith("EOF") || line.startsWith("DISPLAY_DATA_SECTION")) {
                    break;
                }
                else if (read) {
                    String[] tokens = line.split("\\s");
                    
                    for (int i = 0; i < tokens.length; i++)
                    {
                        String v = tokens[i].trim();
                        
                        if (v.length() > 0)
                        {
                            int value = Integer.parseInt(tokens[i].trim());
                            COST[row][column] = value;
                            column++;
                            
                            if (value == 0)
                            {
                                row++;
                                column = 0;
                            }
                        }
                    }
                }
            }
            
            reader.close();
            
            // completes the cost matrix
            for (int i = 0; i < COST.length; i++) {
                for (int j = (i + 1); j < COST.length; j++) {
                    COST[i][j] = COST[j][i];
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Could not load file: " + filename, e);
        }
    }
    
    /**
     * Initialises the population.
     */
    private void initialise() {
        ArrayList<Integer> compList = new ArrayList<Integer>();
        for(int i = 0; i < SIZE; i++) {
            compList.add(i);
        }
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            ArrayList<Integer> cities = new ArrayList(compList);
            for(int i2 = 0; i2 < SIZE; i2++) {
                int place = 0;
                if(cities.size() > 1) {
                    place = random.nextInt(cities.size()-1);
                }
                population[i][i2] = cities.get(place);
                cities.remove(place);
            }
        }
    }
    
    /**
     * Generates a new population from the current.
     * @return The generated population.
     */
    private int[][] generatePopulation() {
        int[][] newPopulation = new int[POPULATION_SIZE][SIZE];
        // Copy current generation best individual (eletism)
        // Generate the rest of the new population
        for(int i = 0; i < POPULATION_SIZE; i++) {
            
        }
        return newPopulation;
    }
    
    /**
     * Returns the index of an individual using tournament selection.
     */
    private int select() {
        int[] contesters = new int[TOURNAMENT_SIZE];
        //Get contesters
        for(int i = 0; i < TOURNAMENT_SIZE; i++) {
            contesters[i] = random.nextInt(POPULATION_SIZE-1);
        }
        int best = contesters[0]; // Set initial individual to beat
        for(int i = 1; i < TOURNAMENT_SIZE; i++) {
            if(fitness[contesters[i]] > fitness[best]) {
                best= contesters[i];
            }
        }
        return best;
    }
    
    /**
     * Calculates the fitness for each individual.
     */
    private void evaluate() {
        for(int i = 0; i < POPULATION_SIZE; i++) {
            int fitness = 0;
            for(int i2 = 0; i2 < SIZE-1; i2++) {
                // Add the cost of this position to the next
                fitness += COST[population[i][i2]][population[i][i2+1]];
            }
            // Add cost of the returning back to the start position
            fitness += COST[population[i][SIZE-1]][population[i][0]];
            this.fitness[i] = fitness; // Se the firness value
        }
    }
    
    /**
     * Combines genetic material from 2 parent individuals.
     * Swaps data using cycle crossover.
     */
    private int[][] crossover(int parent1, int parent2) {
        int[][] children = new int[2][SIZE]; // Array of 2 children to return
        children[0] = copy(population[parent1]); // Copy parent 1
        children[1] = copy(population[parent2]); // Copy parent 2
        int index = random.nextInt(SIZE-1); // Current index
        int startIndex = index; // Store starting index
        // Loop until we get back to the start
        do {
            // Copy data
            children[0][index] = population[parent2][index];
            children[1][index] = population[parent1][index];
            // Find index of parent2 data in parent1
            int search = -1, prevIndex = index;
            // Loop until p2 value found in p1
            do {
                search++;
                if(population[parent1][search] == population[parent2][index]) {
                    prevIndex = index;
                    index = search;
                }
            } while(population[parent1][search] != population[parent2][prevIndex]);
        } while(index != startIndex);
        return children; // Return children
    }
    
    /**
     * 
     * 
     */
    private int[] mutation(int parent) {
        return new int[0];
    }
    
    /**
     * Finds and returns the index ot the highest 
     */
    private int selectBest() {
        return 0;
    }
    
    private String generationStats() {
        return "";
    }
    
    /**
     * @return A copy of the array entered.
     */
    private int[] copy(int[] original) {
        int copy[] = new int[original.length];
        for(int i = 0; i < original.length; i++) {
            copy[i] = original[i];
        }
        return copy;
    }
    
}
