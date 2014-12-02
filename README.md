# sort

Latest jar; sort/dist/lib/psort-0.0.3.jar.

## Parallel sorting playground.


The goal of this project is to create sort algorithms that are faster than the standard Java 8
Collections sort method when on a system with multiple cores, and to make them 
as easy to use as the Collections sort method. I will be optimizing the existing
algorithms as well as adding new ones over the next several months. I may also
venture into other, non-sort parallel algorithms such as graph searching, etc...
Let's see what comes of it!


### The Fastest I've Got

#### Overview:

If you are looking for something to drop into your application that may 
provide a performance boost, the fastest algorithm in the package is wrapped in 
the PCollections class with an API similar to the Collections class. Calling this 
sort method will attempt to determine the number of available processors, and 
sort the given list using the PSRS algorithm described below. Note: this algorithm 
is not 'in-place' (nor is Java's Collections merge sort). If you are seeking more 
control over the number of child processes, or memory usage, you can use various 
other algorithms through their explicitly defined classes.

#### Usage:

```
List<T> sorted = PCollections.sort(List<T> unsorted);
```

#### Performance demonstration:

Some sample data points: compares sorting 20 million randomly generated Integer 
instances via Collections.sort() vs PCollections.sort() on an 8 core system.

+ collections: mean[ 6400 ms ]
+ pcollections: mean[ 2140 ms ]


### Parallel Quicksort

#### Overview:

This is an (mostly) in-place sort that promises linear speedup (2 cores, 2x as fast, etc...) 
up to a small number of cores. The API is generalized to sort lists of
objects that implement the Comparable interface, as well as being able to take a Comparable
instance. I'm currently working on a better pivot selection method. Normal quicksort 
pitfalls apply to this algorithm.

#### Usage:

```
ParallelQuicksort.sort(int P, int minParition, List<T> list);
```

+ P = number of child threads to instantiate.
+ minPartition = smallest range of elements to quicksort. It is more efficient to sort smaller 
lists with a non-quicksort algorithm. Sub-lists smaller than this number will be sorted with
an insertion sort.
+ list = the list to be sorted in-place.


### PSRS

#### Overview:

This is a NON-in-place sort, but it supposedly provides linear speedup up to large
numbers of cores. Given that even desktops are venturing into the 8-core realm, everyday 
performance improvement can be substantial. I have verfied the algorithm's 
performance surpasses Java 8 Collections sort when used with up to 8 cores. This API, too,
is generalized for Comparable/Comparator.

#### Usage:

```
List<Integer> sorted = PSRSSort.sort(int P, List<Integer> unsorted);
```

+ P = number of child threads to instantiate.
+ unsorted = the list to be sorted.
+ Returns sorted = the sorted list.

### Sorting Arrays

I've created a variant of the PSRS sort to work with arrays. This was an attempt to create code that could take advantage of additional cores on my graphics card using [Aparapi](https://code.google.com/p/aparapi/). Unfortunately, the array version of my algorithm is slower than the Arrays.parallelSort() algorithm bundled with Java 8. In the future, I'll be analyzing whether a sort algorithm is amenable to adaptation for execution on a GPU. GPUs seem to be the best environment for 'data parallel' algorithms; i.e. where the same task is performed on numerous instances of data, such as numeric types in an array (think matrix math). However, I'm not sure if it will work well when instances of data need to be examined in relation to one another.

#### Usage:

```
int[] sorted = PSRSSortArray.sort(int P, int[] unsorted);
```
+ P = number of child threads to instantiate.
+ unsorted = the array to be sorted.
+ Returns sorted = the sorted array.
