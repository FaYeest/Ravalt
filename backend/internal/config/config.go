package config

import (
	"fmt"
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

type Config struct {
	ServerPort         string
	Environment        string
	DBHost             string
	DBPort             string
	DBUser             string
	DBPassword         string
	DBName             string
	DBSSLMode          string
	JWTSecret          string
	JWTExpirationHours int
}

func Load() (*Config, error) {
	// Load .env if it exists, don't fail if not found (e.g. in production container)
	_ = godotenv.Load()

	jwtHours, err := strconv.Atoi(getEnv("JWT_EXPIRATION_HOURS", "24"))
	if err != nil {
		jwtHours = 24
	}

	cfg := &Config{
		ServerPort:         getEnv("SERVER_PORT", "8080"),
		Environment:        getEnv("ENVIRONMENT", "development"),
		DBHost:             getEnv("DB_HOST", "localhost"),
		DBPort:             getEnv("DB_PORT", "5432"),
		DBUser:             getEnv("DB_USER", "ravalt_user"),
		DBPassword:         getEnv("DB_PASSWORD", "ravalt_password"),
		DBName:             getEnv("DB_NAME", "ravalt_db"),
		DBSSLMode:          getEnv("DB_SSLMODE", "disable"),
		JWTSecret:          getEnv("JWT_SECRET", "default-dev-secret-change-in-prod"),
		JWTExpirationHours: jwtHours,
	}

	return cfg, nil
}

func (c *Config) DSN() string {
	return fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=%s",
		c.DBUser,
		c.DBPassword,
		c.DBHost,
		c.DBPort,
		c.DBName,
		c.DBSSLMode,
	)
}

func getEnv(key, defaultVal string) string {
	if val, exists := os.LookupEnv(key); exists && val != "" {
		return val
	}
	return defaultVal
}
