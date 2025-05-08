package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemService = new ItemService(itemRepository);
    }

    @Test
    void findAll() {
        List<Item> items = List.of(new Item(1L, "Item1", "Desc1", "NEW", "email@example.com"));
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();

        assertEquals(1, result.size());
        assertEquals("Item1", result.get(0).getName());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void findAllWhenEmpty() {
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        List<Item> result = itemService.findAll();

        assertTrue(result.isEmpty());
        verify(itemRepository).findAll();
    }

    @Test
    void findByIdFound() {
        Item item = new Item(1L, "Item", "Desc", "NEW", "email@example.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Item", result.get().getName());
        verify(itemRepository).findById(1L);
    }

    @Test
    void findByIdNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isEmpty());
        verify(itemRepository).findById(1L);
    }

    @Test
    void save() {
        Item item = new Item(null, "Item", "Desc", "NEW", "email@example.com");
        Item saved = new Item(1L, "Item", "Desc", "NEW", "email@example.com");
        when(itemRepository.save(item)).thenReturn(saved);

        Item result = itemService.save(item);

        assertEquals(1L, result.getId());
        verify(itemRepository).save(item);
    }

    @Test
    void deleteById() {
        itemService.deleteById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void processItemsAsyncSuccess() throws Exception {
        Long id1 = 1L, id2 = 2L;
        Item item1 = new Item(id1, "Item1", "Desc1", "NEW", "email1@example.com");
        Item item2 = new Item(id2, "Item2", "Desc2", "NEW", "email2@example.com");
        Item processed1 = new Item(id1, "Item1", "Desc1", "PROCESSED", "email1@example.com");
        Item processed2 = new Item(id2, "Item2", "Desc2", "PROCESSED", "email2@example.com");

        when(itemRepository.findAllIds()).thenReturn(List.of(id1, id2));
        when(itemRepository.findById(id1)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(id2)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(5, TimeUnit.SECONDS);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> "PROCESSED".equals(i.getStatus())));
        verify(itemRepository, times(2)).findById(anyLong());
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void processItemsAsyncWithMissingItem() throws Exception {
        Long id1 = 1L, id2 = 2L;
        Item item1 = new Item(id1, "Item1", "Desc1", "NEW", "email1@example.com");

        when(itemRepository.findAllIds()).thenReturn(List.of(id1, id2));
        when(itemRepository.findById(id1)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(id2)).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(5, TimeUnit.SECONDS);

        assertEquals(1, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
        verify(itemRepository).findById(id1);
        verify(itemRepository).findById(id2);
        verify(itemRepository).save(item1);
    }

    @Test
    void processItemsAsyncWithRepositoryError() {
        when(itemRepository.findAllIds()).thenReturn(List.of(1L));
        when(itemRepository.findById(1L)).thenThrow(new RuntimeException("DB Error"));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        verify(itemRepository).findById(1L);
    }

    @Test
    void processItemsAsyncWithInterruption() throws Exception {
        Long id = 1L;
        when(itemRepository.findAllIds()).thenReturn(List.of(id));

        // Create a CountDownLatch to synchronize with the interrupt
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Thread> processingThread = new AtomicReference<>();

        when(itemRepository.findById(id)).thenAnswer(invocation -> {
            processingThread.set(Thread.currentThread());
            latch.countDown();
            Thread.sleep(100); // Will be interrupted
            return Optional.empty();
        });

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();

        // Wait for processing to start
        latch.await(5, TimeUnit.SECONDS);

        // Interrupt the processing thread
        processingThread.get().interrupt();

        // Verify the future completes exceptionally
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void shutdownDoesNotThrow() {
        assertDoesNotThrow(() -> itemService.shutdown());
    }

    @Test
    void processItemsAsync_AlsoTestsProcessItem() throws Exception {
        // Acest test acoperă și funcționalitatea processItem
        when(itemRepository.findAllIds()).thenReturn(List.of(1L));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(new Item(1L, "Test", "Desc", "NEW", "test@example.com")));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Item> result = itemService.processItemsAsync().get();
        assertEquals("PROCESSED", result.get(0).getStatus());
    }
}