package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ItemControllerTest {

    private ItemService itemService;
    private ItemController itemController;

    @BeforeEach
    void setUp() {
        itemService = mock(ItemService.class);
        itemController = new ItemController(itemService);
    }

    @Test
    void getAllItems_returnsItems() {
        List<Item> mockItems = List.of(new Item(1L, "Item1", "Desc", "NEW", "test@example.com"));
        when(itemService.findAll()).thenReturn(mockItems);

        ResponseEntity<List<Item>> response = itemController.getAllItems();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockItems, response.getBody());
    }

    @Test
    void getAllItems_returnsNoContent() {
        when(itemService.findAll()).thenReturn(List.of());

        ResponseEntity<List<Item>> response = itemController.getAllItems();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void createItem_validItem_returnsCreated() {
        Item item = new Item(null, "Item", "Desc", "NEW", "email@example.com");
        BindingResult bindingResult = new BeanPropertyBindingResult(item, "item");

        when(itemService.save(item)).thenReturn(item);

        ResponseEntity<?> response = itemController.createItem(item, bindingResult);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(item, response.getBody());
    }

    @Test
    void createItem_invalidItem_returnsBadRequest() {
        Item item = new Item();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        ResponseEntity<?> response = itemController.createItem(item, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation errors: ", response.getBody());
    }

    @Test
    void getItemById_found() {
        Item item = new Item(1L, "Item", "Desc", "NEW", "email@example.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        ResponseEntity<Item> response = itemController.getItemById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(item, response.getBody());
    }

    @Test
    void getItemById_notFound() {
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Item> response = itemController.getItemById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateItem_found() {
        Item item = new Item(null, "Updated", "Desc", "UPDATED", "email@example.com");
        Item updatedItem = new Item(1L, "Updated", "Desc", "UPDATED", "email@example.com");

        when(itemService.findById(1L)).thenReturn(Optional.of(updatedItem));
        when(itemService.save(any(Item.class))).thenReturn(updatedItem);

        ResponseEntity<Item> response = itemController.updateItem(1L, item);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedItem, response.getBody());
    }

    @Test
    void updateItem_notFound() {
        Item item = new Item();
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Item> response = itemController.updateItem(1L, item);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteItem_found() {
        Item item = new Item(1L, "ToDelete", "Desc", "OLD", "email@example.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        doNothing().when(itemService).deleteById(1L);

        ResponseEntity<Void> response = itemController.deleteItem(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteItem_notFound() {
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = itemController.deleteItem(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void processItems_success() {
        List<Item> processed = List.of(new Item(1L, "Done", "Processed", "PROCESSED", "email@example.com"));
        CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(processed);

        when(itemService.processItemsAsync()).thenReturn(future);

        DeferredResult<ResponseEntity<List<Item>>> result = itemController.processItems();
        future.join();

        assertNotNull(result.getResult());
        assertEquals(HttpStatus.OK, ((ResponseEntity<?>) result.getResult()).getStatusCode());
    }

    @Test
    void processItems_failure() {
        CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failure"));

        when(itemService.processItemsAsync()).thenReturn(failedFuture);

        DeferredResult<ResponseEntity<List<Item>>> result = itemController.processItems();
        failedFuture.handle((r, ex) -> null);

        // wait briefly for async handling
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ((ResponseEntity<?>) result.getResult()).getStatusCode());
    }
}
