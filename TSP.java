import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.ArrayList;

/**
 * Simple GA for the Travelling Salesman Problem.
 * 
 * @author Matthew Mansell 
 * @version v1.0
 */
public class TSP
{
    // ########## GA SETTINGS ##########
    private int SIZE; // The number of cities of the TSP instance.
    private int[][] COST; // TSP cost matrix
    private static final int POPULATION_SIZE = 100; // The population size
    private static final int MAX_GENERATION = 19999; // The number of generations
    private static final int TOURNAMENT_SIZE = 5; // The tournament size
    private static final int MUTATION_CHANCE = 5; // Mutation percentage change
    
    private Random random = new Random(); // Accessible random generator
    private int[][] population; // Current population
    private int[] fitness = new int[POPULATION_SIZE]; // Individuals fitness
    private int bestResult; //Store for best result
    private int[][] childrenTest;
    
    public TSP() {
        while(fitness[selectBest()] >= 700 || fitness[selectBest()] == 0) {
            run();
        }
    }
    
    /**
     * Starts the execution of the GA.
     * 
     * @param filename the TSP file instance.
     */
    public void run() {
        String filename = "dantzig.tsp";
        load(filename); // Load the TSP problem cost matrix
        // Print GA run info
        //System.out.println("##### TSP GA #####");
        //System.out.println("Matrix File: "+filename);
        //System.out.println("No. Cities: "+SIZE);
        //System.out.println("Max Generation: "+MAX_GENERATION+"\n");
        
        population = new int[POPULATION_SIZE][SIZE]; // Initialise population now SIZE is set
        initialise(0); // Initialise the population
        evaluate(); // Evaluate the initial population
        // Loop for required generations
        for(int g = 0; g <= MAX_GENERATION; g++) {
            int[][] nextGeneration = generatePopulation(); // Create new population
            population = nextGeneration; // Copy the new population over
            evaluate(); // Evaluate the new population
            //System.out.println("Gen "+g+" | "+generationStats()); // Print stats
        }
        System.out.println("Best Route: "+fitness[selectBest()]); // Print best result
        System.out.println(printRoute(population[selectBest()]));
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
     * Initialises the population with a fixed start
     */
    private void initialise(int startCity) {
        ArrayList<Integer> compList = new ArrayList<Integer>();
        for(int i = 0; i < SIZE; i++) {
            if(i != startCity) {
                compList.add(i);
            }
        }
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            ArrayList<Integer> cities = new ArrayList(compList);
            population[i][0] = startCity;
            for(int i2 = 1; i2 < SIZE-1; i2++) {
                int place = random.nextInt(cities.size()-1);
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
        newPopulation[0] = copy(population[selectBest()]);
        // Generate the rest of the new population
        for(int i = 1; i < POPULATION_SIZE; i++) {
            //Decide to mutate or crossover
            if(random.nextInt(99) > MUTATION_CHANCE-1 && i < POPULATION_SIZE-1) {
                // Generate using crossover
                int children[][] = cycleCrossover(tournamentSelect(),tournamentSelect());
                newPopulation[i] = children[0];
                newPopulation[++i] = children[1];
            } else {
                // Generate with mutation
                newPopulation[i] = inversionMutation(tournamentSelect());
            }
        }
        return newPopulation;
    }
    
    /**
     * Returns the index of an individual using tournament selection.
     */
    private int tournamentSelect() {
        int[] contesters = new int[TOURNAMENT_SIZE];
        //Get contesters
        for(int i = 0; i < TOURNAMENT_SIZE; i++) {
            contesters[i] = random.nextInt(POPULATION_SIZE-1);
        }
        int best = contesters[0]; // Set initial individual to beat
        for(int i = 1; i < TOURNAMENT_SIZE; i++) {
            if(fitness[contesters[i]] < fitness[best]) {
                best= contesters[i];
            }
        }
        return best;
    }
    
    private int rouletteSelect() {
        double[] roulette = new double[POPULATION_SIZE];
        double total = 0;
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            total += fitness[i];
        }
        
        double cumulative = 0.0;
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            roulette[i] = cumulative + (fitness[i] / total);
            cumulative = roulette[i];
        }
        
        roulette[POPULATION_SIZE-1] = 1.0;
        int parent = -1;
        double probability = random.nextDouble();
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            if(probability >= roulette[i]) {
                parent = i;
                break;
            }
        }
        
        return 0;
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
    
    private int[][] partiallyMappedCrossover(int parent1, int parent2) {
        int[][] children = new int[2][SIZE];
        int p1 = random.nextInt(SIZE-2), p2 = 0;
        children[0] = copy(population[parent1]);
        children[1] = copy(population[parent2]);
        return children;
    }
    
    /**
     * Combines genetic material from 2 parent individuals.
     * Swaps data using cycle crossover.
     */
    private int[][] cycleCrossover(int parent1, int parent2) {
        int[][] children = new int[2][SIZE]; // Array of 2 children to return
        children[0] = copy(population[parent1]); // Copy parent 1
        children[1] = copy(population[parent2]); // Copy parent 2
        int index = random.nextInt(SIZE-1); // Index assigned with start value
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
     * Mutation using exchange mutation techinque.
     * @return The mutated child.
     */
    private int[] exchangeMutation(int parent) {
        int[] child = copy(population[parent]); // A copy of the parent
        int p1 = random.nextInt(SIZE-1), p2 = random.nextInt(SIZE-1);
        child[p1] = population[parent][p2]; // Assign value of second point
        child[p2] = population[parent][p1]; // Assign value of first point.
        return child;
    }
    
    /**
     * Mutation using inverison technique.
     * @return The mutated child
     */
    private int[] inversionMutation(int parent) {
        int[] child = copy(population[parent]);
        int p1 = random.nextInt(SIZE-2), p2 = p1+random.nextInt((SIZE-1)-p1);
        if(p1 == p2) {p2++;} // Ensure p2 is larger
        if(p2 == SIZE-1) {
            System.out.println("Works on max");
        }
        for(int i = 0; i <= (p2-p1); i++) {
            //System.out.println("Copying:"+(p1+i)+" to "+(p2-i));
            child[p2-i] = population[parent][p1+i];
        }
        return child;
    }
    
    /**
     * Finds and returns the index ot the highest 
     */
    private int selectBest() {
        int best = 0;
        for(int i = 0; i < POPULATION_SIZE; i++) {
            if(fitness[i] < fitness[best]) {
                best = i;
            }
        }
        return best;
    }
    
    /**
     * @return The stats of the current population.
     */
    private String generationStats() {
        int worst = 0, mean = 0, best = 0;
        for(int i = 0; i < POPULATION_SIZE; i++) {
            mean += fitness[i];
            if(fitness[i] > fitness[worst]) {
                worst = i;
            }
            if(fitness[i] < fitness[best]) {
                best = i;
            }
        }
        mean = mean/POPULATION_SIZE;
        return fitness[worst]+":"+mean+":"+fitness[best];
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
    
    /**
     * @return Route order
     */
    private String printRoute(int[] individual) {
        String returnString = "";
        for(int i = 0; i < SIZE; i++) {
            returnString += (individual[i]+1) + ":";
        }
        returnString += (individual[0]+1); // Print start
        return returnString;
    }
}