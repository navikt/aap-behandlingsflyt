#!/bin/bash

# Find all test files that use MockK, excluding kontrakt module
mockk_test_files=$(find . -name "*.kt" -type f -path "*/test/*" ! -path "*/kontrakt/*" -exec grep -l "import io.mockk" {} \;)

if [ -z "$mockk_test_files" ]; then
  echo "No MockK tests found"
  exit 1
fi

# Extract unique modules
modules=$(echo "$mockk_test_files" | sed 's|^\./||' | sed 's|/src/test/.*||' | sort -u)

# Build test class filters for each module
gradle_commands=""
for module in $modules; do
  test_filters=""
  for file in $mockk_test_files; do
    if [[ "$file" == "./$module/"* ]]; then
      package=$(grep -m 1 "^package " "$file" | sed 's/package //' | sed 's/;$//')
      classname=$(basename "$file" .kt)
      if [ -n "$package" ] && [ -n "$classname" ]; then
        test_filters="$test_filters --tests ${package}.${classname}"
      fi
    fi
  done
  if [ -n "$test_filters" ]; then
    gradle_commands="$gradle_commands :$module:test --rerun $test_filters"
  fi
done

# Run the tests
if [ -n "$gradle_commands" ]; then
  echo "Running MockK tests..."
  ./gradlew $gradle_commands
else
  echo "No MockK tests found"
  exit 1
fi
