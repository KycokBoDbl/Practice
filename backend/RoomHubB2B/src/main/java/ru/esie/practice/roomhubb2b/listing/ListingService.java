package ru.esie.practice.roomhubb2b.listing;

import org.springframework.stereotype.Service;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;
import java.util.List;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public List<ListingResponseDto> getPublishedListings() {
        return listingRepository.findByStatus("PUBLISHED")
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    private ListingResponseDto toResponseDto(ListingEntity listing) {
        return new ListingResponseDto(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerHour(),
                listing.getCapacity(),
                listing.getSpaceType(),
                listing.getImageUrl()
        );
    }
}