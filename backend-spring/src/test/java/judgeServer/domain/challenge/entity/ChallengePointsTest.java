package judgeServer.domain.challenge.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChallengePointsTest {

    private final Challenge challenge = Challenge.builder()
            .maxPoint(100)
            .minPoint(75)
            .decreasePoint(10)
            .build();

    @Test
    void firstSolverGetsMaxPoint() {
        assertEquals(100, challenge.currentPoints(0));
    }

    @Test
    void eachSolveDecreasesPoints() {
        assertEquals(80, challenge.currentPoints(2));
    }

    @Test
    void pointsNeverGoBelowMinimum() {
        assertEquals(75, challenge.currentPoints(3));
        assertEquals(75, challenge.currentPoints(1_000_000));
    }

    @Test
    void zeroDecreaseKeepsPointsFixed() {
        Challenge fixed = Challenge.builder().maxPoint(300).minPoint(0).decreasePoint(0).build();
        assertEquals(300, fixed.currentPoints(50));
    }
}
