package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ExecutorService asyncProcessorExecutor;

    // Constructor injection for the repository and asynchronous processing executor
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        // Using a work-stealing pool for efficient thread usage, which scales well for IO-bound tasks
        this.asyncProcessorExecutor = Executors.newWorkStealingPool(); // More flexible than fixed thread pool
    }

    /**
     * Fetches all items from the repository.
     *
     * @return List of all items
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Fetches an item by its ID.
     *
     * @param id ID of the item
     * @return Optional containing the item if found, empty otherwise
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Saves an item to the repository.
     *
     * @param item Item to be saved
     * @return Saved item
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Deletes an item by its ID.
     *
     * @param id ID of the item to be deleted
     */
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Processes all items asynchronously.
     *
     * <p>This method processes each item by submitting tasks to the executor, which runs the tasks concurrently.</p>
     *
     * @return A CompletableFuture that completes when all items are processed.
     */
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Create all async processing tasks (processing each item in parallel)
        List<CompletableFuture<Item>> processingFutures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> processItem(id), asyncProcessorExecutor))
                .toList();

        // Combine all futures into one that completes when all are done, ensuring all items are processed
        return CompletableFuture.allOf(processingFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processingFutures.stream()
                        .map(CompletableFuture::join) // Wait for all futures to complete
                        .filter(Objects::nonNull) // Filter out any null values if an item processing failed
                        .toList());
    }

    /**
     * Processes a single item by updating its status and saving it to the repository.
     * Handles any exceptions that occur during the processing.
     *
     * @param id ID of the item to process
     * @return The processed item or null if the item could not be found
     */
    private Item processItem(Long id) {
        try {
            // Simulating processing time (e.g., business logic)
            Thread.sleep(100);

            // Fetch the item, update its status, and save it back
            return itemRepository.findById(id)
                    .map(item -> {
                        item.setStatus("PROCESSED"); // Set the processed status
                        return itemRepository.save(item); // Save the item after processing
                    })
                    .orElse(null); // Return null if the item was not found
        } catch (InterruptedException e) {
            // Restore interrupt flag and rethrow as a runtime exception
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        } catch (Exception e) {
            // General exception handling for any other errors during item processing
            throw new RuntimeException("Failed to process item " + id, e);
        }
    }

    /**
     * Cleanup method to shutdown the executor service when the service is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        asyncProcessorExecutor.shutdownNow(); // Stop all tasks and clean up resources
    }
}
// This class is responsible for managing items in the system, including CRUD operations and asynchronous processing.