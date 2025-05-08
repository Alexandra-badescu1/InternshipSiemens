package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.springframework.http.ResponseEntity.*;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return itemService.findAll().isEmpty()
                ? noContent().build()
                : ok(itemService.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        // Validate the request body and return all error messages if there are validation errors
        if (result.hasErrors()) {
            StringBuilder errorMessages = new StringBuilder("Validation errors: ");
            result.getAllErrors().forEach(error -> errorMessages.append(error.getDefaultMessage()).append(" "));
            return badRequest().body(errorMessages.toString());
        }
        return status(HttpStatus.CREATED).body(itemService.save(item));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item) {
        return itemService.findById(id)
                .map(existingItem -> {
                    item.setId(id);
                    return ok(itemService.save(item));
                })
                .orElse(notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (itemService.findById(id).isEmpty()) {
            return notFound().build();
        }
        itemService.deleteById(id);
        return ok().build();
    }

    /**
     * Process items asynchronously and return them once processing is done.
     * Uses DeferredResult to allow non-blocking request handling.
     */
    @GetMapping("/process")
    public DeferredResult<ResponseEntity<List<Item>>> processItems() {
        DeferredResult<ResponseEntity<List<Item>>> result = new DeferredResult<>();

        itemService.processItemsAsync()
                .thenAccept(items -> result.setResult(ok(items)))
                .exceptionally(ex -> {
                    result.setResult(status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                    return null;
                });

        return result;
    }
}
