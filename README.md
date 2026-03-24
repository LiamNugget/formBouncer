# formBouncer

formBouncer is a form spam filtering service built to help developers block low-quality, automated, and suspicious submissions before they ever reach the inbox.

It combines fast rule-based checks with a machine learning layer for deeper classification. The goal is to offer a practical free tier using heuristics, with a more advanced ML-backed option for higher accuracy and stricter filtering. The repository currently contains a Java-based application with supporting Python ML components, plus Docker setup for local development and deployment.

## Why I built it

Contact forms are one of the easiest targets for spam, bot abuse, and low-effort junk submissions. Basic captcha and honeypots help, but they often miss more subtle patterns or create friction for real users.

formBouncer was built to provide a better middle ground:
- low friction for genuine users
- stronger filtering than simple honeypots alone
- a path from lightweight rules to more advanced ML scoring
- easy integration into websites and web apps

## Features

- Heuristic spam detection
- Honeypot support
- Timing-based checks
- Pattern and content analysis
- Machine learning layer for advanced classification
- Docker-based local setup
- Designed to fit both a free tier and premium model

## How it works

formBouncer evaluates incoming form submissions using multiple layers.

### 1. Heuristic checks

The first stage is fast rule-based filtering. This can include checks such as:
- hidden field or honeypot triggers
- suspicious submission timing
- obvious spam phrases or patterns
- malformed or low-quality input
- scoring based on common abuse signals

This layer is intended to be lightweight and practical enough for a standard free tier.

### 2. ML classification

For more advanced filtering, formBouncer can pass submissions through a trained machine learning model. This allows the system to go beyond simple fixed rules and classify messages based on broader patterns learned from training data.

This repository includes an `ml` directory and processed data structure, indicating the project is built with a separate ML workflow alongside the main application.

### 3. Final scoring

The final decision can be based on combined scoring from heuristics and ML output, allowing flexible thresholds depending on how aggressive the filtering should be.

## Tech stack

Based on the current repository structure, formBouncer uses:
- Java for the main application
- Python for machine learning components
- Docker and Docker Compose for containerised setup
- Maven for Java project management

The repository currently includes `src`, `ml`, `data/processed`, `pom.xml`, `Dockerfile`, and `docker-compose.yml`.

## Repository structure

```text
formBouncer/
├── data/
│   └── processed/
├── ml/
├── src/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Getting started

### Prerequisites

Make sure you have:

- Java
- Maven
- Python
- Docker
- Docker Compose

You may not need every tool for every workflow, but these are the main technologies currently present in the repository.

### Run locally with Docker

```bash
docker compose up --build
```
Run the Java app with Maven
```
mvn clean install
mvn spring-boot:run
```

If your project is not using Spring Boot, replace the second command with your actual entrypoint command.

Machine learning workflow

The ML layer is intended to improve detection accuracy beyond fixed heuristics. A typical workflow would be:

collect and label form submission data
clean and process the dataset
train a classifier
evaluate model performance
export the model for use in the main application
combine ML output with heuristic scoring
Example use case

A website sends form submission data to formBouncer before storing or emailing it.

formBouncer then:

checks for honeypot triggers
evaluates timing and pattern-based rules
optionally runs ML classification
returns a spam score or allow/block decision

That lets the site decide whether to:

accept the message
flag it
silently drop it
route it for manual review
Project status

This project is a work in progress.

The current aim is to build a practical spam filtering service that is:

useful as a standalone API or service
easy to integrate into websites
scalable from simple heuristics to more advanced ML filtering
commercially viable with both free and premium capability
Roadmap
Improve heuristic scoring rules
Add clearer API documentation
Add request and response examples
Expand model training and evaluation
Introduce configurable thresholds
Add authentication and usage limits
Add dashboard or reporting features
Publish versioned releases
Potential use cases
contact forms
lead generation forms
support request forms
registration and enquiry flows
internal business tools that accept user-generated text
Contributing

This project is currently under active development. If you want to experiment, fork the repository and open an issue or pull request with improvements.
