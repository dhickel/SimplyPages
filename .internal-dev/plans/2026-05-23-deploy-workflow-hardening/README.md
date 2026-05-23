# Deploy Workflow Hardening Plan

## Context

The current `./deploy.sh host 192.168.1.113 host` workflow builds the project locally and then
copies the packaged demo jar over SSH. During the editing/demo contract PR validation, local build
and tests passed, but deployment stopped at the transfer step because SSH to `192.168.1.113:22`
was refused.

## Goals

- Make remote deployment failures explicit and actionable.
- Split build, transfer, service restart, and smoke verification into separate phases with clear
  exit codes.
- Avoid relying on manual curl checks that can silently test the wrong host or stale service.
- Document the expected SSH identity, known-host behavior, and remote service layout.

## Proposed Implementation

1. Add a deploy preflight phase.
   - Verify target host and port are reachable before running the full Maven build.
   - Verify SSH authentication succeeds with the configured user.
   - Print the resolved target path and service name.

2. Split deployment phases.
   - `build`: run clean package and verify Javadocs are packaged.
   - `transfer`: copy the exact jar and print its checksum.
   - `restart`: stop/start or restart the configured service.
   - `smoke`: verify the remote HTTP endpoint from the local machine after restart.

3. Make smoke checks contract-based.
   - Check `/editing-demo` returns 200 and contains the expected page marker.
   - Check one framework CSS asset and one WebJar asset return 200.
   - Check a representative HTMX endpoint returns the expected fragment marker.

4. Document remote prerequisites.
   - SSH user and key.
   - Required Java runtime.
   - Remote directory ownership.
   - Service manager command and log location.

5. Consolidate deploy-related bugs after replacement behavior is specified.
   - Merge duplicate "remote curl/deploy smoke" issues into this plan.
   - Keep any host availability incident as a separate dated note if it recurs.

## Validation

- Simulate unreachable SSH and verify the script stops before building.
- Run a normal deployment against the live host when SSH is available.
- Verify smoke checks fail against a stopped service and pass against the restarted service.

## Out Of Scope

- Replacing the live host or changing application runtime packaging.
- Adding production-grade release orchestration.
