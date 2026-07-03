package ru.esie.practice.roomhubb2b.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "roomhub.booking")
public class BookingProperties {

    @NotNull
    private Duration confirmationHold = Duration.ofMinutes(30);

    @Min(1)
    private int batchSize = 100;

    public Duration confirmationHold() {
        return confirmationHold;
    }

    public void setConfirmationHold(Duration confirmationHold) {
        this.confirmationHold = confirmationHold;
    }

    public int batchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @AssertTrue(message = "roomhub.booking.confirmation-hold must be positive")
    public boolean isConfirmationHoldPositive() {
        return confirmationHold != null && !confirmationHold.isNegative() && !confirmationHold.isZero();
    }
}
