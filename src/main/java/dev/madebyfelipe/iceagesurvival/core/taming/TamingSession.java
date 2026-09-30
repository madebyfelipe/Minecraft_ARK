package dev.madebyfelipe.iceagesurvival.core.taming;

/**
 * Andamento de uma domesticação em curso: quanto a criatura já comeu, com que
 * qualidade, e quanto dano sofreu enquanto estava inconsciente.
 */
public final class TamingSession {
    private double foodValue;
    private double qualityWeighted;
    private double damageFraction;

    public TamingSession() {
    }

    public TamingSession(double foodValue, double qualityWeighted, double damageFraction) {
        this.foodValue = Math.max(0, foodValue);
        this.qualityWeighted = Math.max(0, qualityWeighted);
        this.damageFraction = Math.max(0, damageFraction);
    }

    /**
     * @param value   quanto este alimento avança a domesticação
     * @param quality de 0 a 1; 1 é o alimento preferido da espécie
     */
    public void feed(double value, double quality) {
        if (value <= 0) {
            throw new IllegalArgumentException("Valor de alimento inválido: " + value);
        }
        foodValue += value;
        qualityWeighted += value * Math.clamp(quality, 0.0, 1.0);
    }

    /** @param fractionOfMaxHealth dano sofrido, como fração da vida máxima */
    public void recordDamage(double fractionOfMaxHealth) {
        damageFraction += Math.max(0, fractionOfMaxHealth);
    }

    /** De 0 a 1. */
    public double progress(double requiredFood) {
        return requiredFood <= 0 ? 1.0 : Math.min(1.0, foodValue / requiredFood);
    }

    public boolean isComplete(double requiredFood) {
        return foodValue >= requiredFood;
    }

    /**
     * De 0 a 1: qualidade média do que foi comido, reduzida pelo dano sofrido.
     * Perder toda a vida máxima em dano zera a eficiência.
     */
    public double effectiveness() {
        if (foodValue <= 0) {
            return 0.0;
        }
        return (qualityWeighted / foodValue) * (1.0 - Math.min(1.0, damageFraction));
    }

    public void reset() {
        foodValue = 0;
        qualityWeighted = 0;
        damageFraction = 0;
    }

    public double foodValue() {
        return foodValue;
    }

    public double qualityWeighted() {
        return qualityWeighted;
    }

    public double damageFraction() {
        return damageFraction;
    }
}
