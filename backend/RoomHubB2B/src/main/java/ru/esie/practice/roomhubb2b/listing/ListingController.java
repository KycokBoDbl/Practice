package ru.esie.practice.roomhubb2b.listing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

import java.util.List;

@RestController
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/api/listings")
    public List<ListingResponseDto> getListings() {
        return listingService.getPublishedListings();
    }
}