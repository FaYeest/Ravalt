package main

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/FaYeest/ravalt/backend/internal/config"
	"github.com/FaYeest/ravalt/backend/internal/handler"
	repoPostgres "github.com/FaYeest/ravalt/backend/internal/repository/postgres"
	"github.com/FaYeest/ravalt/backend/internal/service"
	"github.com/FaYeest/ravalt/backend/internal/storage/postgres"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
)

type HealthResponse struct {
	Status    string    `json:"status"`
	Database  string    `json:"database"`
	Timestamp time.Time `json:"timestamp"`
}

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	// Connect to PostgreSQL Pool
	dbPool, err := postgres.NewPool(ctx, cfg.DSN())
	if err != nil {
		log.Fatalf("Failed to initialize database pool: %v", err)
	}
	defer dbPool.Close()

	log.Printf("Successfully connected to PostgreSQL at %s:%s/%s", cfg.DBHost, cfg.DBPort, cfg.DBName)

	// Repositories
	userRepo := repoPostgres.NewUserRepository(dbPool)

	// Services
	authService := service.NewAuthService(userRepo, cfg.JWTSecret)

	// Handlers
	authHandler := handler.NewAuthHandler(authService)

	r := chi.NewRouter()

	// Global Middlewares
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.Timeout(30 * time.Second))

	// CORS Configuration for Mobile & Web clients
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	// API Routes
	r.Route("/api/v1", func(r chi.Router) {
		r.Route("/auth", func(r chi.Router) {
			r.Get("/prelogin", authHandler.Prelogin)
			r.Post("/register", authHandler.Register)
		})
	})

	// Health Check Route
	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")

		dbStatus := "connected"
		pingCtx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()

		if err := dbPool.Ping(pingCtx); err != nil {
			dbStatus = "disconnected"
			w.WriteHeader(http.StatusServiceUnavailable)
		} else {
			w.WriteHeader(http.StatusOK)
		}

		_ = json.NewEncoder(w).Encode(HealthResponse{
			Status:    "ok",
			Database:  dbStatus,
			Timestamp: time.Now().UTC(),
		})
	})

	server := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      r,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		log.Printf("Server is running on port %s in %s mode", cfg.ServerPort, cfg.Environment)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("Server failed to listen: %v", err)
		}
	}()

	// Graceful Shutdown
	<-ctx.Done()
	log.Println("Shutting down server gracefully...")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("Server forced to shutdown: %v", err)
	}

	log.Println("Server exited cleanly.")
}
