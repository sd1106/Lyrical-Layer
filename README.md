# Lyrical Layer

**Lyrical Layer** is a predictive writing assistant and interactive lyric guessing game powered by a custom Trie data structure. Built to assist artists in composing music in specific styles, the application allows users to draft lines using pattern prediction modeled after **Taylor Swift** and **Kendrick Lamar**.

---

## Features

* **Artist Style Toggle:** Switch between Taylor Swift and Kendrick Lamar dataset models in real time.
* **Real-Time Predictive Text:**
  * Displays the single most likely next character based on current prefix context.
  * Predicts the most likely full next word.
  * Displays the top 5 next word predictions accompanied by occurrence percentages.
* **Live Database Validation:** Visual color indicators show whether user input matches sequences in the database (Green for match, Red for no match).
* **Lyric Guessing Game:** Interactive mode where users fill in missing words from authentic artist lyrics.
* **Tailored User Interface:** Theme changes dynamically to match the selected artist.

---

## Data Pipeline and Processing

1. **File Reading:** Standard `NIO` file reader ingests raw `.txt` lyric files line by line.
2. **Filtering:** Filters out blank lines and bracketed song section headers such as `[Verse 1]` or `[Chorus]`.
3. **Text Sanitization:** Uses standard regex matching (`replaceAll("[^a-zA-Z'\\s]", "")`) to strip punctuation and non-lyrical symbols, converting text to lowercase for consistent matching.
4. **Trie Insertion:** Tokenizes lines by spaces and tabs, inserting each word into the Trie while preserving full line entries for the guessing game.

---

## System Architecture and Trie Implementation

The system utilizes a prefix tree where every node maintains traversal metrics:

### Trie Node Specification
* **Pass Count (`passCount`):** Integer tracking how many words traverse through the node.
* **End Count (`endCount`):** Integer tracking how many words terminate at the node to capture word frequencies.
* **Children Map:** Stored using `HashMap<Character, Node>`.

### Prediction Algorithms
* **`mostLikelyNextChar`:** Traverses to the node corresponding to the prefix and returns the child node with the maximum `passCount`.
* **`mostLikelyNextWord`:** Follows the path of highest `passCount` until reaching a valid word termination node (`endCount > 0`).

---

## Algorithmic Complexity Analysis

| Operation | Time Complexity | Details |
| :--- | :--- | :--- |
| **Insertion (`insertLine`)** | $O(L)$ | Iterates over length $L$ of the word being inserted into the Trie. |
| **Lookup (`contains`)** | $O(L)$ | Traverses character by character over length $L$ of the search word. |
| **Next Character Prediction** | $O(L)$ | Navigates down the Trie path matching the $L$-length prefix. |
| **Next Word Predictions** | $O(L)$ | Locates the prefix node in $O(L)$ time before identifying high-frequency complete words. |

---
