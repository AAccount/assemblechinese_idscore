# Assembly Backend

## Overview
This repo has 2 parts: an ids file parser, and backend service. Unlike the dictionary's cedict parser, the ids parser is extremely purpose built for this utility. Its results is almost certainly not reusable anywhere else.

## Parser
The IDS parser only extracts the "parts list" of a character. It entierly discards any positional information. Given the input to the backend is a 1 dimensional string, inventing a 2d notation for using positional info would be horribly complicated and make this program too painful to use. 

The parser also does a lot of "homogenizing" of parts that look the same to someone with only an English background. See the `PART_SWAP` table in `IdsParser`, but there is no sense in using a special character for 訁when it is really 言. This likely has usage in font rendering, but is counterproductive for "assembling" purposes.

For better results, the parser will recursively decompose a parts list by 1 level if possible. Recursing too many levels will eventually end up with a list of "atomic primitive parts" which is not useful for this program. Inputting a long list of atomics is going to be very painful. 1 was chosen as a balance of simpler parts on hand vs list of atomics.

For practicality, the parser only parses lines in the unicode "main block" and "extension a". Exotic characters in the >= "extension b" are ignored as they are apparently found in some classical poetry, literary anthologies, etc, none of which I could understand anyways.

## Backend
DbService acts as the api the outside world interacts with. DbRepo handles all the actual database reading and writing. Given the simple nature of this tool vs the dictionary, it has no database cache. 

The 2 "APIs" in `DbService`: `lookupByParts` (for assembly) and `getPartsFor` (disassembly) both work with standard strings. Assembly is where you give a list of parts you recognize from an unknown Chinese character, and the backend will search the database for all characters made of that list of parts. Disassembly is where you give **a single** character to get a list of parts it is made of. The intention is for you to copy and paste the parts of interest into assembly mode. If more than 1 character is given to assembly mode, everything after the 1st is ignored.
