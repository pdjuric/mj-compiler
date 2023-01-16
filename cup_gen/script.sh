
grammar=$(sed 's:/\*(\(.*\))\*/:(\1):' cup_gen/template.cup | sed 's:/\*.*\*/::' | sed 's://.*$::' | sed 's:^.*[^;|]$:& |:');
temp_grammar=$(echo "$grammar" | sed 's/{:.*:}//');

t_ignore=$(echo "$temp_grammar" | grep -w "terminal" | tr -d ",;" | tr " " "\n" | sort -u);
n_ignore=$(echo "$temp_grammar" | grep -w "nonterminal" | tr -d ",;" | tr " " "\n" | sort -u);

t_pattern="[A-Z_]+";
n_pattern="[A-Z][a-zA-Z_]+";


temp_grammar=$(echo "$temp_grammar" | grep -vE "terminal|nonterminal")
n_defined=$(echo "$temp_grammar" | grep -E "^\ *$n_pattern\ *::=" | sed 's:^\ *\([A-Z][a-zA-Z]*\).*:\1:')


temp_grammar=$(echo "$temp_grammar" | tr -s " " "\n" | tr -d ";" | sed 's/:[a-zA-Z]*//g' | sort -u)

t_list=$(echo "$temp_grammar" | grep -E "^$t_pattern$")
t_string=$t_list
n_list=$(echo "$temp_grammar" | grep -E "^$n_pattern$" | grep -vE "${t_string// /\\|}")

t_removed=$(echo -e "$t_list\n$t_ignore" | sort | uniq -d)
n_removed=$(echo -e "$n_list\n$n_ignore" | sort | uniq -d)

n_undefined=$(echo -e "$n_list\n$n_defined" | sort | uniq -u)

t_list=$(echo -e "$t_list\n$t_removed" | sort | uniq -u | sed 's:^\(.*\)$:terminal \1;:')
n_list=$(echo -e "$n_list\n$n_removed" | sort | uniq -u | sed 's:^\(.*\)$:nonterminal \1;:')

echo removed terminals: $t_removed
echo unused, but explicitly declared terminals: $(echo -e "$t_ignore\n$t_removed" | sort | uniq -u)
echo removed nonterminals: $n_removed
echo unused, but explicitly declared nonterminals: $(echo -e "$n_ignore\n$n_removed" | sort | uniq -u)
echo used, but undefined nonterminals: $n_undefined

cp cup_gen/cup_prefix.txt spec/mjparser.cup;
(echo "$t_list"; echo "$n_list"; echo "$grammar") >>spec/mjparser.cup;


