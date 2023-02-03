package org.workcraft.plugins.parity;

/**
 * OinkOutputNode represents part of the solution generated by the Oink backend.
 * Collectively an ArrayList of output nodes will represent the whole output
 * model. With this, it is possible to colour the game appropriately depending
 * on winning vertices and winning strategies.
 */
public class OinkOutputNode {

    /**
     * Automatically determined identifier. User does NOT decide the identifier;
     * this is separate to the name of the node in the workcraft model.
     */
    Integer id;

    /**
     * Determines if Player 1 has won the game or not.
     * False if Player 0 won, True if Player 1 won
     */
    Boolean wonByPlayer1;

    /**
     * Winning strategy for the given output vertex.
     * Will be set to -1 if there is no winning strategy, otherwise this will
     * be an identifier of another vertex.
     */
    Integer strategy = -1;

    /**
     * Constructor if the current vertex does not hold a winning strategy.
     * @param id
     * @param wonByPlayer1
     */
    public OinkOutputNode(Integer id, Boolean wonByPlayer1) {
        this.id = id;
        this.wonByPlayer1 = wonByPlayer1;
    }

    /**
     * Constructor if the current vertex does hold a winning strategy.
     * @param id
     * @param wonByPlayer1
     * @param strategy
     */
    public OinkOutputNode(Integer id, Boolean wonByPlayer1, Integer strategy) {
        this.id = id;
        this.wonByPlayer1 = wonByPlayer1;
        this.strategy = strategy;
    }

    /**
     * Get the identifier of an output node
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Get the Boolean stating if Player 1 is the winner or not
     * @return wonByPlayer1
     */
    public Boolean getWonByPlayer1() {
        return wonByPlayer1;
    }

    /**
     * Get the winning strategy for the given vertex.
     * @return strategy
     */
    public Integer getStrategy() {
        return strategy;
    }

    //TEST FUNCTION: Return a string of all the attributes
    @Override
    public String toString() {
        return "ID: " + id + " Won by Player 1? " + wonByPlayer1 + " Strategy: " + strategy;
    }
}