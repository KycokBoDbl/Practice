package ru.esie.practice.roomhubb2b.listing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import ru.esie.practice.roomhubb2b.auth.OrganizationEntity;
import ru.esie.practice.roomhubb2b.auth.OrganizationRepository;
import ru.esie.practice.roomhubb2b.auth.UserRole;
import ru.esie.practice.roomhubb2b.booking.BookingRepository;
import ru.esie.practice.roomhubb2b.listing.dto.CreateListingRequestDto;
import ru.esie.practice.roomhubb2b.listing.dto.ListingResponseDto;
import ru.esie.practice.roomhubb2b.listing.dto.UpdateListingRequestDto;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final OrganizationRepository organizationRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ListingService(
            ListingRepository listingRepository,
            OrganizationRepository organizationRepository,
            BookingRepository bookingRepository,
            Clock clock
    ) {
        this.listingRepository = listingRepository;
        this.organizationRepository = organizationRepository;
        this.bookingRepository = bookingRepository;
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

    @Transactional
    public ListingResponseDto update(ListingActor actor, Long listingId, UpdateListingRequestDto request) {
        ListingEntity listing = loadOwnedListing(actor, listingId);
        listing.updateDetails(
                request.title(),
                request.description(),
                request.city(),
                request.address(),
                request.pricePerHour(),
                request.capacity(),
                request.spaceType(),
                request.imageUrl()
        );
        log.info("Listing {} updated by organization {}", listing.getId(), actor.organizationId());
        return toResponseDto(listing);
    }

    @Transactional
    public void hide(ListingActor actor, Long listingId) {
        ListingEntity listing = loadOwnedListing(actor, listingId);
        listing.archive();
        log.info("Listing {} archived by organization {}", listing.getId(), actor.organizationId());
    }

    @Transactional
    public void activate(ListingActor actor, Long listingId) {
        ListingEntity listing = loadOwnedListing(actor, listingId);
        listing.activate();
        log.info("Listing {} activated by organization {}", listing.getId(), actor.organizationId());
    }

    @Transactional
    public void delete(ListingActor actor, Long listingId) {
        ListingEntity listing = loadOwnedListingForDelete(actor, listingId);
        if (bookingRepository.existsByListingId(listing.getId())) {
            throw new ListingConflictException("Listing has booking history and cannot be deleted");
        }
        try {
            listingRepository.delete(listing);
            listingRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ListingConflictException("Listing has booking history and cannot be deleted");
        }
        log.info("Listing {} deleted by organization {}", listing.getId(), actor.organizationId());
    }

    private ListingEntity loadOwnedListingForDelete(ListingActor actor, Long listingId) {
        if (actor.role() != UserRole.LANDLORD) {
            throw new ListingForbiddenException("Role " + actor.role() + " cannot manage listings");
        }
        ListingEntity listing = listingRepository.findLockedById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found"));
        OrganizationEntity owner = listing.getOwnerOrganization();
        if (owner == null || !owner.getId().equals(actor.organizationId())) {
            throw new ListingNotFoundException("Listing not found");
        }
        return listing;
    }

    private ListingEntity loadOwnedListing(ListingActor actor, Long listingId) {
        if (actor.role() != UserRole.LANDLORD) {
            throw new ListingForbiddenException("Role " + actor.role() + " cannot manage listings");
        }
        return listingRepository.findByIdAndOwnerOrganizationId(listingId, actor.organizationId())
                .orElseThrow(() -> new ListingNotFoundException("Listing not found"));
    }

    private ListingResponseDto toResponseDto(ListingEntity listing) {
        OrganizationEntity owner = listing.getOwnerOrganization();
        return new ListingResponseDto(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getPricePerHour(),
                listing.getCapacity(),
                listing.getSpaceType(),
                listing.getImageUrl(),
                listing.getDescription(),
                listing.getAddress(),
                owner == null ? null : owner.getLegalName()
        );
    }
}
