package com.axxes.traineeship.threading.demo.part3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

public class GameOfLife {

    private static final int SIZE = 50;
    private static final int NUMBER_OF_STEPS = 100;
    private static final int NUMBER_OF_THREADS = 10;        // SIZE should be evenly divisible by NUMBER_OF_THREADS;

    private boolean[][] board;
    private int step = 0;
    private boolean debug = true;

    private GameOfLife() {
        board = createEmptyChunk(SIZE);

        // Line of 1s in middle
        for (int y = SIZE / 2 - 25; y < SIZE / 2 + 25; y++) {
            board[SIZE / 2][y] = true;
        }

        // Line of three
        // board[2][2] = true;
        // board[2][3] = true;
        // board[2][4] = true;
    }

    public static void main(String[] args) throws InterruptedException {
        GameOfLife game = new GameOfLife();
        game.log();
        long start = System.currentTimeMillis();
        game.simulateParallelled();
        long end = System.currentTimeMillis();
        System.out.println("Duration: " + (end - start));
    }

    private void simulateSequentially() {
        for (int n = 1; n <= NUMBER_OF_STEPS; n++) {
            boolean[][] chunk = simulationStep(createEmptyChunk(SIZE), SIZE, 0);
            combineChunks(chunk);
        }
    }

    private void simulateParallelled() throws InterruptedException {
        int chunkSize = SIZE / NUMBER_OF_THREADS;
        boolean[][][] calculatedChunks = new boolean[NUMBER_OF_THREADS][SIZE][chunkSize];

        CyclicBarrier barrier = new CyclicBarrier(NUMBER_OF_THREADS, () -> combineChunks(calculatedChunks));
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            int chunk = i;
            threads.add(new Thread(() -> {
                for (int j = 0; j < NUMBER_OF_STEPS; j++) {
                    try {
                        calculatedChunks[chunk] = simulationStep(createEmptyChunk(chunkSize), chunkSize, chunk * chunkSize);
                        barrier.await();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * Executes a step in the simulation for a specified chunk.
     *
     * @param targetChunk the chunk to be simulated
     * @param chunkSize   the width of the chunk
     * @param startY      the y-coordinate of the board with which the beginning of the chunk corresponds
     * @return
     */
    private boolean[][] simulationStep(boolean[][] targetChunk, int chunkSize, int startY) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < chunkSize; y++) {
                targetChunk[x][y] = calculateNewValue(x, startY + y);
            }
        }
        return targetChunk;
    }

    /**
     * Combines the chunks generated by the previous simulation step into the new board state.
     * This method should be called between each simulation step.
     */
    private synchronized void combineChunks(boolean[][]... chunks) {
        int chunkSize = SIZE / chunks.length;
        for (int i = 0; i < chunks.length; i++) {
            boolean[][] chunk = chunks[i];
            for (int x = 0; x < SIZE; x++) {
                System.arraycopy(chunk[x], 0, board[x], i * chunkSize, chunkSize);
            }
        }
        log();
    }

    /**
     * Creates a chunk with the specified width. All elements of the chunk are false.
     */
    private boolean[][] createEmptyChunk(int width) {
        boolean[][] newBoard = new boolean[SIZE][width];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < width; y++) {
                newBoard[x][y] = false;
            }
        }
        return newBoard;
    }

    private boolean calculateNewValue(int x, int y) {
        List<Boolean> neighbours = new ArrayList<>();
        getValue(x - 1, y - 1).ifPresent(neighbours::add);
        getValue(x - 1, y).ifPresent(neighbours::add);
        getValue(x - 1, y + 1).ifPresent(neighbours::add);
        getValue(x, y - 1).ifPresent(neighbours::add);
        getValue(x, y + 1).ifPresent(neighbours::add);
        getValue(x + 1, y - 1).ifPresent(neighbours::add);
        getValue(x + 1, y).ifPresent(neighbours::add);
        getValue(x + 1, y + 1).ifPresent(neighbours::add);
        long count = neighbours.stream().filter(v -> v).count();
        if (board[x][y]) {
            return 2 == count || count == 3;
        } else {
            return count == 3;
        }
    }

    private Optional<Boolean> getValue(int x, int y) {
        try {
            return Optional.of(board[x][y]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            return Optional.empty();
        }
    }

    private void log() {
        System.out.println("STEP " + step++);
        if (debug) {
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    if (board[x][y]) {
                        System.out.print(1 + " ");
                    } else {
                        System.out.print(0 + " ");
                    }
                }
                System.out.println();
            }
            System.out.println("---------------------------------------");
        }
    }
}