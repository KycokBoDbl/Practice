package ru.esie.practice.roomhubb2b.listing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final OrganizationRepository organizationRepository;
    private final Clock clock;

    public ListingService(
            ListingRepository listingRepository,
            OrganizationRepository organizationRepository,
            Clock clock
    ) {
        this.listingRepository = listingRepository;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
    }

    public List<ListingResponseDto> getPublishedListings() {
        return listingRepository.findByStatus(ListingStatus.PUBLISHED)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public ListingResponseDto publish(ListingActor actor, CreateListingRequestDto request) {
        if (actor.role() != UserRole.LANDLORD) {
            throw new ListingForbiddenException("Role " + actor.role() + " cannot publish listings");
        }
        OrganizationEntity owner = organizationRepository.findById(actor.organizationId())
                .orElseThrow(() -> new ListingForbiddenException("Landlord organization not found"));
        LocalDateTime createdAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        ListingEntity listing = ListingEntity.published(
                request.title(),
                request.description(),
                request.city(),
                request.address(),
                request.pricePerHour(),
                request.capacity(),
                request.spaceType(),
                request.imageUrl(),
                createdAt,
                owner
        );
        ListingEntity saved = listingRepository.save(listing);
        log.info("Listing {} published by organization {}", saved.getId(), actor.organizationId());
        return toResponseDto(saved);
    }

    private ListingResponseDto toResponseDto(ListingEntity listing) {
        return new ListingResponseDto(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerHour(),
                listing.getCapacity(),
                listing.getSpaceType(),
                listing.getImageUrl(),
                listing.getDescription(),
                listing.getAddress()
        );
    }
}
