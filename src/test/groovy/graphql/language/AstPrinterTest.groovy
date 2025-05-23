package graphql.language

import graphql.parser.Parser
import spock.lang.Specification

import java.nio.CharBuffer

class AstPrinterTest extends Specification {

    Document parse(String input) {
        new Parser().parseDocument(input)
    }

    String printAst(String input) {
        Document document = parse(input)

        AstPrinter.printAst(document)
    }

    String printAst(Node node) {
        AstPrinter.printAst(node)
    }

    boolean isParseableAst(ast) {
        try {
            def document = parse(ast)
            return document != null
        } catch (Exception ignored) {
            return false
        }
    }


    def starWarsSchema = """
# objects can have comments
# over a number of lines
schema {
    query: QueryType
    mutation: Mutation
}

type QueryType {
    # the hero of the film
    hero(episode: Episode): Character
    human(id : String) : Human
    droid(id: ID!): Droid
}

type Mutation {
    createReview(episode: Episode, review: ReviewInput): Review
}

enum Episode {
    NEWHOPE
    EMPIRE
    JEDI
}

interface Character {
    id: ID!
    name: String!
    friends: [Character]
    appearsIn: [Episode]!
}

interface Node {
  id: ID!
}

type Human implements Character & Node {
    id: ID!
    name: String!
    friends: [Character]
    appearsIn: [Episode]!
    homePlanet: String
}

type Droid implements Character & Node {
    id: ID!
    name: String!
    friends: [Character]
    appearsIn: [Episode]!
    primaryFunction: String
}

union SearchResult = Human | Droid | Starship

type Review implements Node {
  id: ID!
  stars: Int!
  commentary: String
}

input ReviewInput {
  stars: Int!
  commentary: String
}

scalar DateTime
"""

    //-------------------------------------------------
    def "ast printing a complete schema"() {
        String output = printAst(starWarsSchema)

        expect:
        //
        // notice how it tightens everything up
        //
        output == """schema {
  query: QueryType
  mutation: Mutation
}

type QueryType {
  hero(episode: Episode): Character
  human(id: String): Human
  droid(id: ID!): Droid
}

type Mutation {
  createReview(episode: Episode, review: ReviewInput): Review
}

enum Episode {
  NEWHOPE
  EMPIRE
  JEDI
}

interface Character {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
}

interface Node {
  id: ID!
}

type Human implements Character & Node {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
  homePlanet: String
}

type Droid implements Character & Node {
  id: ID!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
  primaryFunction: String
}

union SearchResult = Human | Droid | Starship

type Review implements Node {
  id: ID!
  stars: Int!
  commentary: String
}

input ReviewInput {
  stars: Int!
  commentary: String
}

scalar DateTime
"""
    }

    //-------------------------------------------------
    def "ast printing specific schema node"() {
        def document = parse(starWarsSchema)
        String output = printAst(document.getDefinitions().get(0))

        expect:
        output == """schema {
  query: QueryType
  mutation: Mutation
}"""
    }

    def "ast printing specific type node"() {
        def document = parse(starWarsSchema)
        String output = printAst(document.getDefinitions().get(1))

        expect:
        output == """type QueryType {
  hero(episode: Episode): Character
  human(id: String): Human
  droid(id: ID!): Droid
}"""
    }

    //-------------------------------------------------
    def "ast printing of queries"() {
        def query = """
{
  empireHero: hero(episode: EMPIRE) {
    name
  }
  jediHero: hero(episode: JEDI) {
    name
  }
}"""
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == """{
  empireHero: hero(episode: EMPIRE) {
    name
  }
  jediHero: hero(episode: JEDI) {
    name
  }
}
"""

    }

    //-------------------------------------------------
    def "ast printing of fragments"() {
        def query = """
{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}
"""
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == """{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}
"""
    }

//-------------------------------------------------
    def "ast printing of variables"() {
        def query = '''
query HeroNameAndFriends($episode: Episode) {
  hero(episode: $episode) {
    name
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query HeroNameAndFriends($episode: Episode) {
  hero(episode: $episode) {
    name
  }
}
'''
    }

//-------------------------------------------------
    def "ast printing of directives"() {
        def query = '''
query Hero($episode: Episode, $withFriends: Boolean!) {
  hero ( episode: $episode) {
    name @repeatable @repeatable
    friends @include (if : $withFriends) {
      name
    }
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        isParseableAst(output)
        output == '''query Hero($episode: Episode, $withFriends: Boolean!) {
  hero(episode: $episode) {
    name @repeatable @repeatable
    friends @include(if: $withFriends) {
      name
    }
  }
}
'''
    }

//-------------------------------------------------
    def "ast printing of inline fragments"() {
        def query = '''
query HeroForEpisode($ep: Episode!) {
  hero(episode: $ep) {
    name
       ... on Droid {
        primaryFunction
     }
         ... on Human {
      height
    }
  }
}'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query HeroForEpisode($ep: Episode!) {
  hero(episode: $ep) {
    name
    ... on Droid {
      primaryFunction
    }
    ... on Human {
      height
    }
  }
}
'''
        isParseableAst(output)
    }

    def "ast printing of inline fragments in compactMode"() {
        def query = '''
query HeroForEpisode($ep: Episode!) {
  hero(episode: $ep) {
    name
       ... on Droid {
        primaryFunction
        id
     }
         ... on Human {
        height
        id
    }
    age {
      inMonths
      inYears
    }
  }
}'''
        when:
        def document = parse(query)
        String output = AstPrinter.printAstCompact(document)

        then:

        isParseableAst(output)

        output == 'query HeroForEpisode($ep:Episode!){hero(episode:$ep){name ...on Droid{primaryFunction id}...on Human{height id}age{inMonths inYears}}}'
    }


//-------------------------------------------------
    def "ast printing of default variables"() {
        def query = '''
query HeroNameAndFriends($episode: Episode = "JEDI") {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query HeroNameAndFriends($episode: Episode = "JEDI") {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
'''
    }

//-------------------------------------------------
    def "ast printing of null"() {
        def query = '''
query NullEpisodeQuery {
  hero(episode: null) {
    name
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query NullEpisodeQuery {
  hero(episode: null) {
    name
  }
}
'''
    }
//-------------------------------------------------
    def "ast printing of empty string"() {
        def query = '''
query NullEpisodeQuery {
  human(id: "") {
    name
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query NullEpisodeQuery {
  human(id: "") {
    name
  }
}
'''
    }

    def "ast printing of blank string"() {
        def query = '''
query NullEpisodeQuery {
  human(id: "     ") {
    name
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query NullEpisodeQuery {
  human(id: "     ") {
    name
  }
}
'''
    }

    //-------------------------------------------------
    def "ast printing of default variables with null"() {
        def query = '''
query NullVariableDefaultValueQuery($episode: Episode = null) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''query NullVariableDefaultValueQuery($episode: Episode = null) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
'''
    }

    def "print arguments descriptions"() {
        def query = '''
type Query {
    field(
    #description1
    arg1: String,
    arg2: String,
    #description3
    arg3: String
    ): String
}
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''type Query {
  field(arg1: String, arg2: String, arg3: String): String
}
'''

    }

    def "print field descriptions"() {
        def query = '''type Query {
  "description"
  field(
  "description"
  a: String): String
}
'''
        def document = parse(query)
        String output = printAst(document)
        expect:
        output == '''type Query {
  "description"
  field(
  "description"
  a: String): String
}
'''
    }

    def "print empty description"() {
        def query = '''
""
scalar Demo
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''""
scalar Demo
'''
    }

    def "print type extensions"() {
        def query = '''
    extend schema {
        query: Query
    }
    
    extend type Object @directive {
        objectField : String
    }    

    extend interface Interface @directive {
        objectField : String
    }    

    extend union Union @directive = | Foo | Baz
    
    extend enum Enum {
        X
        Y
    }
    
    extend scalar Scalar @directive

    extend input Input @directive {
        inputField : String
    }
'''
        def document = parse(query)
        String output = printAst(document)

        expect:
        output == '''extend schema {
  query: Query
}

extend type Object @directive {
  objectField: String
}

extend interface Interface @directive {
  objectField: String
}

extend union Union @directive = Foo | Baz

extend enum Enum {
  X
  Y
}

extend scalar Scalar @directive

extend input Input @directive {
  inputField: String
}
'''

    }

    def "compact ast printing"() {
        def query = '''
    { 
        #comments go away
        aliasOfFoo : foo(arg1 : "val1", args2 : "val2") @isCached @orIsItNotCached     { #   and this comment as well
            hello
        } 
        world @neverCache @youSaidCache @okThenCache 
    }
    
    fragment FX on SomeType {
        aliased : field(withArgs : "argVal", andMoreArgs : "andMoreVals")
    }
'''
        def document = parse(query)
        String output = AstPrinter.printAstCompact(document)

        expect:
        isParseableAst(output)
        output == '''{aliasOfFoo:foo(arg1:"val1",args2:"val2") @isCached@orIsItNotCached{hello}world @neverCache@youSaidCache@okThenCache} fragment FX on SomeType {aliased:field(withArgs:"argVal",andMoreArgs:"andMoreVals")}'''
    }

    def "can tighten fields with no query prefix"() {
        when:
        def doc = parse("{root { fooA{ midB{ leafB}} fooB{ midB{ leafB         }}}}")
        def output = AstPrinter.printAstCompact(doc)
        then:
        isParseableAst(output)
        output == "{root{fooA{midB{leafB}}fooB{midB{leafB}}}}"
    }

    def "print ast with inline fragment without type condition"() {
        def query = '''
    { 
        foo {
            ... {
                hello
            }
        }
    }
'''
        def document = parse(query)
        String outputCompact = AstPrinter.printAstCompact(document)
        String outputFull = AstPrinter.printAst(document)

        expect:
        outputCompact == '''{foo{...{hello}}}'''
        outputFull == '''{
  foo {
    ... {
      hello
    }
  }
}
'''
        isParseableAst(outputCompact)
        isParseableAst(outputFull)
    }

    def 'StringValue is converted to valid Strings'() {

        when:
        def result = AstPrinter.printAstCompact(new StringValue(strValue))

        then:
        result == expected

        where:
        strValue            | expected
        'VALUE'             | '"VALUE"'
        'VA\n\t\f\n\b\\LUE' | '"VA\\n\\t\\f\\n\\b\\\\LUE"'
        'VA\\L"UE'          | '"VA\\\\L\\"UE"'
    }

    def 'Interfaces implementing interfaces'() {
        given:
        def interfaceType = InterfaceTypeDefinition
                .newInterfaceTypeDefinition()
                .name("Resource")
                .implementz(new TypeName("Node"))
                .implementz(new TypeName("Extra"))
                .build()


        when:
        def result = AstPrinter.printAstCompact(interfaceType)

        then:
        result == "interface Resource implements Node & Extra"

    }

    def 'Interfaces implementing interfaces in extension'() {
        given:
        def interfaceType = InterfaceTypeExtensionDefinition
                .newInterfaceTypeExtensionDefinition()
                .name("Resource")
                .implementz(new TypeName("Node"))
                .implementz(new TypeName("Extra"))
                .build()

        when:
        def result = AstPrinter.printAstCompact(interfaceType)

        then:
        result == "extend interface Resource implements Node & Extra"

    }

    def "directive definitions can be printed"() {

        given:
        def directiveDef1 = DirectiveDefinition.newDirectiveDefinition()
                .name("d1")
                .repeatable(true)
                .directiveLocation(DirectiveLocation.newDirectiveLocation().name("FIELD").build())
                .directiveLocation(DirectiveLocation.newDirectiveLocation().name("OBJECT").build())
                .build()

        def directiveDef2 = DirectiveDefinition.newDirectiveDefinition()
                .name("d2")
                .repeatable(false)
                .directiveLocation(DirectiveLocation.newDirectiveLocation().name("FIELD").build())
                .directiveLocation(DirectiveLocation.newDirectiveLocation().name("ENUM").build())
                .build()

        when:
        def result = AstPrinter.printAstCompact(directiveDef1)

        then:
        result == "directive @d1 repeatable on FIELD | OBJECT"

        when:
        result = AstPrinter.printAstCompact(directiveDef2)

        then:
        result == "directive @d2 on FIELD | ENUM"

    }

    def "empty type does not include braces"() {
        def sdl = "type Query"
        def document = parse(sdl)

        when:
        String output = printAst(document)
        then:
        output == "type Query\n"
    }

    def "empty selection set does not include braces"() {
        // technically below is not valid graphql and will never be parsed as is
        def field_with_empty_selection_set = Field.newField("foo")
                .selectionSet(SelectionSet.newSelectionSet().build())
                .build()

        when:
        String output = printAst(field_with_empty_selection_set)
        then:
        output == "foo"
    }

    def "printAstTo writes to a StringBuilder instance"() {
        def document = parse(starWarsSchema)
        def output = new StringBuilder()
        AstPrinter.printAstTo(document.getDefinitions().get(0), output)

        expect:
        output.toString() == """schema {
  query: QueryType
  mutation: Mutation
}"""
    }

    def "printAstTo writes to a Writer instance"() {
        def document = parse(starWarsSchema)
        def output = new StringWriter()
        AstPrinter.printAstTo(document.getDefinitions().get(0), output)

        expect:
        output.toString() == """schema {
  query: QueryType
  mutation: Mutation
}"""
    }

    def "printAstTo writes to an Appendable instance"() {
        def document = parse(starWarsSchema)
        def output = CharBuffer.allocate(100)
        AstPrinter.printAstTo(document.getDefinitions().get(0), output)
        output.flip()

        expect:
        output.toString() == """schema {
  query: QueryType
  mutation: Mutation
}"""
    }
}
