import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.ArrayList;

/**
 * Simple GA for the Travelling Salesman Problem.
 * View the optimum/best results for each tsp in the README.
 * View the notes on optimal configuration in the word file.
 * 
 * Assignment task completion:
 * Task 1:
 *  - Initialisation of individuals: see initialise().
 *  - Fitness function: see evaluate().
 *  - Mutation operator: see inversionMutation().
 *  - Crossover operator: see cycleCrossover().
 *  - Selection method: see tournamentSelect().
 *  - Evolutionary loop: see run() and generatePopulation().
 *  - Fixed city start: see initialise() and all other methods prevented from 
 *    altering index 0.
 *  Task 2:
 *  - Alternate mutation operator: see exchangeMutation().
 *  - Alternative crossover operator: see partiallyMappedCrossover().
 *  - Alternative selection method: see rouletteSelect();
 *  Task 3: see included word document.
 * 
 * @author Matthew Mansell 
 * @version v2.0
 */
public class TSP_GA
{
    // ########## CONSTANTS ##########
    private static final int TOURNAMENT_SELECT = 0;
    private static final int ROULETTE_SELECT = 1;
    private static final int EXCHANGE_MUTATION = 0;
    private static final int INVERSION_MUTATION = 1;
    private static final int PM_CROSSOVER = 0;
    private static final int CYCLE_CROSSOVER = 1;
    private static final int SELECTION_METHOD = TOURNAMENT_SELECT; // The selection method
    private static final int MUTATION_METHOD = INVERSION_MUTATION; 
    private static final int CROSSOVER_METHOD = CYCLE_CROSSOVER;
    private static final int POPULATION_SIZE = 150; // The population size
    private static final int GENERATIONS = 5000; // The number of generations
    private static final int TOURNAMENT_SIZE = 5; // The tournament size
    private static final int MUTATION_CHANCE = 5; // Mutation percentage change
    
    // ########## VARIABLES ##########
    private int SIZE; // The number of cities of the TSP instance.
    private int[][] COST; // TSP cost matrix
    private Random random = new Random(); // Accessible random generator
    private int[][] population; // Current population
    private int[] fitness = new int[POPULATION_SIZE]; // Individuals fitness
    private int bestResult; //Store for best 15result
    private int[][] childrenTest;
    
    /**
     * @param tsbFile An initial tsb file to be loaded.
     */
    public TSP_GA(String tsbFile) {
        load(tsbFile);
    }
    
    /**
     * Starts the execution of the GA / The GA's main evolutionary loop.
     * @param printEachGen Set true if you want to see generation stats.
     */
    public void run(boolean printEachGen) {
        initialise(); // Initialise the population
        evaluate(); // Evaluate the initial population
        // Loop for required generations
        for(int g = 0; g < GENERATIONS; g++) {
            int[][] nextGeneration = generatePopulation(); // Create new population
            population = nextGeneration; // Copy the new population over
            evaluate(); // Evaluate the new population
            if(printEachGen) {
                System.out.println("Gen "+g+" | "+generationStats()); // Print stats
                System.out.println(printRoute(population[selectBest()]));
            }
        }
        //Print the best result
        System.out.println("Best Route: "+fitness[selectBest()]);
        System.out.println(printRoute(population[selectBest()]));
    }
    
    /**
     * Runs the GA repeatedly until the input goal, or better, is found.
     * WARNING: This method will run endlesly if the goal input is not acheivable.
     * @param goal The value to run until.
     */
    public void runUntil(int goal) {
        System.out.println("RUNNING UNTIL "+goal);
        int runs = 0, total = 0;
        long startTime = System.currentTimeMillis(), endTime; //Start time
        do {
            run(false);
            runs++;
            total += fitness[selectBest()];
        } while(fitness[selectBest()] > goal || fitness[selectBest()] == 0);
        endTime = System.currentTimeMillis(); //End time
        System.out.println("Found value ("+goal+") after "+runs+" runs");
        System.out.println("Average result: "+(total/runs));
        System.out.println("Execution time:"+(endTime-startTime)+"ms");
    }
    
    /**
     * Runs the GA for the entered number of runs.
     * @param runs The number of times of which to run the GA.
     */
    public void runFor(int runs) {
        System.out.println("RUNNING FOR "+runs);
        int total = 0, best = 0;
        long startTime = System.currentTimeMillis(), endTime; //Start time
        for(int i = 0; i < runs; i++) {
            run(false);
            total += fitness[selectBest()];
            if(fitness[selectBest()] < best || best == 0) {
                best = fitness[selectBest()];
            }
        }
        endTime = System.currentTimeMillis(); //End time
        System.out.println("Best result in "+runs+" runs: "+best);
        System.out.println("Average result: "+(total/runs));
        System.out.println("Execution time:"+(endTime-startTime)+"ms");
    }
    
    /**
     * Loads the TSP file. This method will initialise the variables SIZE and COST.
     * @param filename The name of the TSB file to be loaded.
     */
    public void load(String filename) {
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
        // Print GA run info
        System.out.println("##### TSP #####");
        System.out.println("Matrix File: "+filename);
        System.out.println("No. Cities: "+SIZE);
    }
    
    /**
     * Initialises the population, with a fixed start of 0 (city 1).
     */
    private void initialise() {
        population = new int[POPULATION_SIZE][SIZE]; //Re-initialise population
        //compList is a store of all cities that can be added to an individual.
        ArrayList<Integer> compList = new ArrayList<Integer>();
        for(int i = 1; i < SIZE; i++) {
            compList.add(i);
        }
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            //Copy compList so the original is no changed.
            ArrayList<Integer> cities = new ArrayList(compList);
            population[i][0] = 0; // Add fixed start city
            for(int i2 = 1; i2 < SIZE; i2++) {
                int place = random.nextInt(cities.size());
                population[i][i2] = cities.get(place);
                cities.remove(place); //Ensure we can no longer pick this city
            }
        }
    }
    
    /**
     * Generates a new population from the current.
     * Switch cases are used to decide what methods to use, as specified in the 
     * constants. Switch cases allow for easy addition of methods in the future if 
     * required.
     * @return The generated population.
     */
    private int[][] generatePopulation() {
        int[][] newPopulation = new int[POPULATION_SIZE][SIZE];
        // Copy current generation best individual (eletism)
        newPopulation[0] = copy(population[selectBest()]);
        // Generate the rest of the new population
        for(int i = 1; i < POPULATION_SIZE; i++) {
            //Select 2 parents, though mutation will only use one.
            int[] parents = new int[2];
            switch(SELECTION_METHOD) {
                case TOURNAMENT_SELECT:
                    parents[0] = tournamentSelect();
                    parents[1] = tournamentSelect();
                    break;
                case ROULETTE_SELECT:
                    parents[0] = rouletteSelect();
                    parents[1] = rouletteSelect();
                    break;
                default:
                    parents[0] = tournamentSelect();
                    parents[1] = tournamentSelect();
                    break;
            }
            
            //Decide to mutate or crossover
            if(random.nextInt(99) > MUTATION_CHANCE-1 && i < POPULATION_SIZE-1) {
                //CROSSOVER
                int[][] children;
                switch(CROSSOVER_METHOD) {
                    case CYCLE_CROSSOVER:
                        children = cycleCrossover(parents);
                        break;
                    case PM_CROSSOVER:
                        children = partiallyMappedCrossover(parents);
                        break;
                    default:
                        children = cycleCrossover(parents); // Default to cycle crossover
                        break;
                }
                newPopulation[i] = children[0];
                newPopulation[++i] = children[1]; //Increment i additional value
            } else {
                //MUTATION
                int[] child;
                switch(MUTATION_METHOD) {
                    case EXCHANGE_MUTATION:
                        child = exchangeMutation(parents[0]);
                        break;
                    case INVERSION_MUTATION:
                        child = inversionMutation(parents[0]);
                        break;
                    default:
                        child = inversionMutation(parents[0]); // Default to inversion mutation
                        break;
                }
                newPopulation[i] = child;
            }
        }
        return newPopulation;
    }
    
    /**
     * Selection method using a tournament technique.
     * @return The index of the selected individual.
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
    
    /**
     * Selection method using a roulette technique.
     * Roulette selection encourages maximisation and so is incompatible with TSP.
     * I have implemented a method of reversing the values so that a greater degree
     * of proportionality is kept, and encouraging minimisation.
     * @return The index of the selected individual.
     */
    private int rouletteSelect() {
        double[] roulette = new double[POPULATION_SIZE];
        int[] inverseFitness = new int[POPULATION_SIZE];
        double total = 0;
        int max = 0, min = 0;
        //Find max and min values
        for(int i = 0; i < POPULATION_SIZE; i++) {
            total += fitness[i];
            if(fitness[i] > max) {
                max = fitness[i];
            }
            if(fitness[i] < min || min == 0) {
                min = fitness[i];
            }
        }
        
        //Reverse the proportionality of each individual
        int x = max+min;
        for(int i = 0; i < POPULATION_SIZE; i++) {
            inverseFitness[i] = x-fitness[i];
        }
        
        //Calculate total
        for(int i = 0; i < POPULATION_SIZE; i++) {
            total += inverseFitness[i];
        }
        
        double cumulative = 0.0;
        for(int i = 0; i < POPULATION_SIZE; i++) {
            roulette[i] = cumulative + (inverseFitness[i] / total);
            cumulative = roulette[i];
        }
        
        roulette[POPULATION_SIZE-1] = 1.0;
        int parent = -1;
        double probability = random.nextDouble();
        
        for(int i = 0; i < POPULATION_SIZE; i++) {
            if(probability <= roulette[i]) {
                parent = i;
                break;
            }
        }
        
        return parent;
    }
    
    /**
     * Calculates the fitness values for each individual.
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
     * Combines genetic material from 2 parent individuals using a partial map
     * crossover technique.
     * @param parents The individuals to crossover from.
     * @return The 2 child individuals.
     */
    private int[][] partiallyMappedCrossover(int[] parents) {
        int[][] children = new int[2][SIZE];
        int p1 = random.nextInt(SIZE-2)+1, p2 = p1+random.nextInt((SIZE-1)-p1);
        if(p1 == p2) {p2++;}
        //Copy the mapping section
        for(int i = p1; i <= p2; i++) {
            children[0][i] = population[parents[1]][i];
            children[1][i] = population[parents[0]][i];
        }
        
        //Fill the rest of the children
        for(int i = 0; i < 2; i++) { //For both children
            children[i][0] = population[parents[i]][0]; //Copy first value directly
            for(int i2 = 1; i2 < SIZE; i2++) { //For each value
                if(children[i][i2] == 0) { //If the value is empty
                    //Check if value already exists in the child
                    int searchIndex = i2;
                    boolean canAdd; //Assume true
                    do {
                        canAdd = true;
                        for(int i3 = 1; i3 < SIZE; i3++) {
                            if(children[i][i3] == population[parents[i]][searchIndex]) {
                                canAdd = false; //Found the value exists already
                                searchIndex = i3;
                                break;
                            }
                        }
                    } while(!canAdd);
                    
                    if(canAdd) { //Add the value
                        children[i][i2] = population[parents[i]][searchIndex];
                    }
                }
            }
        }
        return children;
    }
    
    /**
     * Combines genetic material from 2 parent individuals using a cycle crossover
     * technique.
     * @param parents The individuals to crossover from.
     * @return The 2 child individuals.
     */
    private int[][] cycleCrossover(int parents[]) {
        int[][] children = new int[2][SIZE]; // Array of 2 children to return
        children[0] = copy(population[parents[0]]); // Copy parent 1
        children[1] = copy(population[parents[1]]); // Copy parent 2
        int index = random.nextInt(SIZE-2)+1; // Index assigned with start value, ensuring its not 0 by +1
        int startIndex = index; // Store starting index
        // Loop until we get back to the start
        do {
            // Copy data
            children[0][index] = population[parents[1]][index];
            children[1][index] = population[parents[0]][index];
            // Find index of parent2 data in parent1
            int search = -1, prevIndex = index;
            // Loop until p2 value found in p1
            do {
                search++;
                if(population[parents[0]][search] == population[parents[1]][index]) {
                    prevIndex = index;
                    index = search;
                }
            } while(population[parents[0]][search] != population[parents[1]][prevIndex]);
        } while(index != startIndex);
        return children; // Return children
    }
    
    /**
     * Mutation using exchange mutation techinque.
     * @param parent The individual to mutate from.
     * @return The mutated child.
     */
    private int[] exchangeMutation(int parent) {
        int[] child = copy(population[parent]); // A copy of the parent
        int p1 = random.nextInt(SIZE-2)+1, p2 = p1+random.nextInt((SIZE-1)-p1);
        if(p1 == p2) {p2++;} // Ensure p2 is larger
        child[p1] = population[parent][p2]; // Assign value of second point
        child[p2] = population[parent][p1]; // Assign value of first point.
        return child;
    }
    
    /**
     * Mutation using inverison technique.
     * @param parent The individual to mutate from.
     * @return The mutated child
     */
    private int[] inversionMutation(int parent) {
        int[] child = copy(population[parent]);
        int p1 = random.nextInt(SIZE-2)+1, p2 = p1+random.nextInt((SIZE-1)-p1);
        if(p1 == p2) {p2++;} // Ensure p2 is larger
        
        for(int i = 0; i <= (p2-p1); i++) {
            //System.out.println("Copying:"+(p1+i)+" to "+(p2-i));
            child[p2-i] = population[parent][p1+i];
        }
        return child;
    }
    
    /**
     * @return The index of the best individual.
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
     * @return Route order
     */
    private String printRoute(int[] individual) {
        String returnString = "";
        for(int i = 0; i < SIZE; i++) {
            returnString += (individual[i]+1) + ":";
        }
        returnString += (individual[0]+1);
        return returnString;
    }
}